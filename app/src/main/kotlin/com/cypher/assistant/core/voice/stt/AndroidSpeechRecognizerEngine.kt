package com.cypher.assistant.core.voice.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.cypher.assistant.core.voice.VoiceState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CypherSTT"

/**
 * Production-grade SpeechRecognizer implementation for Cypher AI.
 *
 * Designed with:
 * - Robust error classification (treating NO_MATCH/TIMEOUT as recoverable non-fatal events)
 * - Safe instance recycling on ERROR_RECOGNIZER_BUSY & SERVER_DISCONNECTED
 * - Main-Looper bound execution guarantees
 * - Audio-reactive RMS forwarding for visualizer animations
 * - Instant partial result delivery for low-latency wake-word triggers
 */
@Singleton
class AndroidSpeechRecognizerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechRecognizerEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isCreating = false

    private val _state = MutableStateFlow(VoiceState.IDLE)
    override val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    override val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _partialText = MutableStateFlow("")
    override val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    override val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _recognizedEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    override val recognizedEvents: SharedFlow<String> = _recognizedEvents.asSharedFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 3)
    override val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val _silenceTimeoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 3)
    override val silenceTimeoutEvents: SharedFlow<Unit> = _silenceTimeoutEvents.asSharedFlow()

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    private fun getOrCreateRecognizer(): SpeechRecognizer? {
        if (speechRecognizer == null && !isCreating) {
            isCreating = true
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
                Log.d(TAG, "CYPHER_VOICE: Recognizer created")
            } catch (e: Exception) {
                Log.e(TAG, "CYPHER_VOICE: Failed to create SpeechRecognizer instance", e)
                speechRecognizer = null
            } finally {
                isCreating = false
            }
        }
        return speechRecognizer
    }

    override fun startListening(locale: Locale, preferOffline: Boolean) {
        mainHandler.post {
            try {
                if (!isAvailable()) {
                    val msg = "Speech recognition service is unavailable on this device."
                    Log.e(TAG, "CYPHER_VOICE: $msg")
                    _state.value = VoiceState.ERROR
                    scope.launch { _errorEvents.emit(msg) }
                    return@post
                }

                // If currently active, cancel previous session cleanly
                if (isListening) {
                    try {
                        speechRecognizer?.cancel()
                    } catch (e: Exception) {
                        Log.w(TAG, "CYPHER_VOICE: Exception cancelling prior recognition session", e)
                    }
                    isListening = false
                }

                val recognizer = getOrCreateRecognizer()
                if (recognizer == null) {
                    val msg = "Could not initialize speech recognizer."
                    Log.e(TAG, "CYPHER_VOICE: $msg")
                    _state.value = VoiceState.ERROR
                    scope.launch { _errorEvents.emit(msg) }
                    return@post
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    if (preferOffline) {
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    }
                }

                _recognizedText.value = ""
                _partialText.value = ""
                _rmsLevel.value = 0f
                isListening = true

                recognizer.startListening(intent)
                Log.d(TAG, "CYPHER_VOICE: Starting listening (locale=${locale.toLanguageTag()}, offline=$preferOffline)")
            } catch (e: Exception) {
                Log.e(TAG, "CYPHER_VOICE: Failed to start speech recognition", e)
                isListening = false
                _state.value = VoiceState.IDLE
                safeDestroy()
                scope.launch { _silenceTimeoutEvents.emit(Unit) }
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                isListening = false
                speechRecognizer?.stopListening()
                Log.d(TAG, "CYPHER_VOICE: stopListening called")
            } catch (e: Exception) {
                Log.e(TAG, "CYPHER_VOICE: Error stopping SpeechRecognizer", e)
            }
        }
    }

    override fun cancel() {
        mainHandler.post {
            try {
                isListening = false
                speechRecognizer?.cancel()
                _rmsLevel.value = 0f
                _partialText.value = ""
                Log.d(TAG, "CYPHER_VOICE: cancel called")
            } catch (e: Exception) {
                Log.e(TAG, "CYPHER_VOICE: Error cancelling SpeechRecognizer", e)
            }
        }
    }

    private fun safeDestroy() {
        try {
            speechRecognizer?.destroy()
            Log.d(TAG, "CYPHER_VOICE: Recognizer destroyed safely")
        } catch (e: Exception) {
            Log.w(TAG, "CYPHER_VOICE: Exception during SpeechRecognizer destruction", e)
        } finally {
            speechRecognizer = null
            isListening = false
        }
    }

    override fun destroy() {
        mainHandler.post {
            safeDestroy()
            _rmsLevel.value = 0f
            _partialText.value = ""
            _state.value = VoiceState.IDLE
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "CYPHER_VOICE: onReadyForSpeech")
            isListening = true
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "CYPHER_VOICE: onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize typical RMS range [-2.0dB .. 10.0dB] to [0.0 .. 1.0]
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _rmsLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "CYPHER_VOICE: onEndOfSpeech")
            isListening = false
            _rmsLevel.value = 0f
        }

        override fun onError(errorCode: Int) {
            isListening = false
            _rmsLevel.value = 0f

            when (errorCode) {
                // Normal silence/timeout events in Android SpeechRecognizer
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    Log.d(TAG, "CYPHER_VOICE: SpeechRecognizer silence/timeout (code=$errorCode). Non-fatal, continuing loop.")
                    scope.launch { _silenceTimeoutEvents.emit(Unit) }
                }

                // Speech service busy -> Safely recycle instance and notify loop
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    Log.w(TAG, "CYPHER_VOICE: SpeechRecognizer busy (code=8). Resetting instance.")
                    safeDestroy()
                    scope.launch { _silenceTimeoutEvents.emit(Unit) }
                }

                // Server or client disconnection -> Reset instance and notify loop
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
                SpeechRecognizer.ERROR_CLIENT -> {
                    Log.w(TAG, "CYPHER_VOICE: SpeechRecognizer client/server issue (code=$errorCode). Resetting instance.")
                    safeDestroy()
                    scope.launch { _silenceTimeoutEvents.emit(Unit) }
                }

                // Permission error -> User-facing
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    val msg = "Microphone permission is required."
                    Log.e(TAG, "CYPHER_VOICE: $msg (code=$errorCode)")
                    _state.value = VoiceState.ERROR
                    scope.launch { _errorEvents.emit(msg) }
                }

                // Network error -> User-facing recoverable warning
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    val msg = "Network connection issue with speech service."
                    Log.w(TAG, "CYPHER_VOICE: $msg (code=$errorCode)")
                    scope.launch {
                        _errorEvents.emit(msg)
                        _silenceTimeoutEvents.emit(Unit)
                    }
                }

                else -> {
                    Log.w(TAG, "CYPHER_VOICE: SpeechRecognizer error code: $errorCode")
                    safeDestroy()
                    scope.launch { _silenceTimeoutEvents.emit(Unit) }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            _rmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim().orEmpty()
            Log.d(TAG, "CYPHER_VOICE: Final result = $text (candidates=${matches?.size})")

            _recognizedText.value = text
            _partialText.value = text

            if (text.isNotBlank()) {
                scope.launch { _recognizedEvents.emit(text) }
            } else {
                scope.launch { _silenceTimeoutEvents.emit(Unit) }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotBlank()) {
                Log.d(TAG, "CYPHER_VOICE: Partial result = $partial")
                _partialText.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
