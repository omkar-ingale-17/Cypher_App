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

/**
 * Production-ready SpeechRecognizerEngine implementation for Android.
 * Enforces single SpeechRecognizer instance lifecycle, controlled continuous re-arming,
 * main-thread looper safety, and structured lifecycle logging.
 */
@Singleton
class AndroidSpeechRecognizerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechRecognizerEngine {

    companion object {
        private const val TAG = "CYPHER_VOICE"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening: Boolean = false
    private var isContinuousActive: Boolean = false
    private var isDestroyed: Boolean = false

    private var currentLocale: Locale = Locale.getDefault()
    private var currentPreferOffline: Boolean = false

    private val _state = MutableStateFlow(VoiceState.IDLE)
    override val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    override val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _partialText = MutableStateFlow("")
    override val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    override val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _recognizedEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val recognizedEvents: SharedFlow<String> = _recognizedEvents.asSharedFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val _silenceTimeoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    override val silenceTimeoutEvents: SharedFlow<Unit> = _silenceTimeoutEvents.asSharedFlow()

    private val rearmRunnable = Runnable {
        executeRearm()
    }

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun getOrCreateRecognizer(): SpeechRecognizer? {
        if (speechRecognizer != null) return speechRecognizer
        return try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer.setRecognitionListener(createRecognitionListener())
            speechRecognizer = recognizer
            Log.i(TAG, "CYPHER_VOICE: RECOGNIZER_CREATED")
            recognizer
        } catch (e: Exception) {
            Log.e(TAG, "CYPHER_VOICE: Failed to create SpeechRecognizer", e)
            null
        }
    }

    private fun buildRecognizerIntent(locale: Locale, preferOffline: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
        }
    }

    override fun startListening(
        locale: Locale,
        preferOffline: Boolean
    ) {
        currentLocale = locale
        currentPreferOffline = preferOffline
        isContinuousActive = true
        isDestroyed = false

        mainHandler.removeCallbacks(rearmRunnable)
        mainHandler.post {
            if (isDestroyed || !isContinuousActive) return@post
            try {
                // Cancel any previous session to ensure clean state
                try { speechRecognizer?.cancel() } catch (_: Exception) {}

                val recognizer = getOrCreateRecognizer() ?: run {
                    Log.e(TAG, "CYPHER_VOICE: Speech recognition not available on device")
                    scope.launch { _errorEvents.emit("Speech recognition not available") }
                    return@post
                }

                val intent = buildRecognizerIntent(locale, preferOffline)
                isListening = true
                Log.i(TAG, "CYPHER_VOICE: MIC_ON")
                Log.i(TAG, "CYPHER_VOICE: LISTENING_STARTED (locale=${locale.toLanguageTag()})")
                recognizer.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "CYPHER_VOICE: Error starting SpeechRecognizer", e)
                isListening = false
                rearmContinuously(delayMs = 250L)
            }
        }
    }

    /**
     * Seamlessly and safely re-arms the recognition session for continuous listening.
     */
    fun rearmContinuously(delayMs: Long = 0L) {
        if (isDestroyed || !isContinuousActive) return

        mainHandler.removeCallbacks(rearmRunnable)

        if (delayMs <= 0L) {
            mainHandler.post(rearmRunnable)
        } else {
            mainHandler.postDelayed(rearmRunnable, delayMs)
        }
    }

    private fun executeRearm() {
        if (isDestroyed || !isContinuousActive) return
        isListening = false
        try {
            // Cancel active session before re-arming
            try { speechRecognizer?.cancel() } catch (_: Exception) {}

            val recognizer = getOrCreateRecognizer() ?: return
            val intent = buildRecognizerIntent(currentLocale, currentPreferOffline)
            isListening = true
            Log.i(TAG, "CYPHER_VOICE: RECOGNIZER_RESTART")
            Log.i(TAG, "CYPHER_VOICE: MIC_ON")
            Log.i(TAG, "CYPHER_VOICE: LISTENING_STARTED")
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.w(TAG, "CYPHER_VOICE: Error during recognizer re-arm, recreating recognizer in 300ms", e)
            isListening = false
            safeDestroy()
            if (!isDestroyed && isContinuousActive) {
                mainHandler.postDelayed(rearmRunnable, 300L)
            }
        }
    }

    override fun stopListening() {
        isContinuousActive = false
        mainHandler.removeCallbacks(rearmRunnable)
        mainHandler.post {
            try {
                isListening = false
                speechRecognizer?.stopListening()
                Log.i(TAG, "CYPHER_VOICE: MIC_OFF")
                Log.i(TAG, "CYPHER_VOICE: LISTENING_ENDED")
            } catch (e: Exception) {
                Log.e(TAG, "CYPHER_VOICE: Error stopping SpeechRecognizer", e)
            }
        }
    }

    override fun cancel() {
        isContinuousActive = false
        mainHandler.removeCallbacks(rearmRunnable)
        mainHandler.post {
            try {
                isListening = false
                speechRecognizer?.cancel()
                _rmsLevel.value = 0f
                _partialText.value = ""
                Log.i(TAG, "CYPHER_VOICE: MIC_OFF")
                Log.i(TAG, "CYPHER_VOICE: LISTENING_ENDED (cancelled)")
            } catch (e: Exception) {
                Log.e(TAG, "CYPHER_VOICE: Error cancelling SpeechRecognizer", e)
            }
        }
    }

    private fun safeDestroy() {
        try {
            speechRecognizer?.destroy()
            Log.i(TAG, "CYPHER_VOICE: RECOGNIZER_DESTROYED")
        } catch (e: Exception) {
            Log.w(TAG, "CYPHER_VOICE: Exception during SpeechRecognizer destruction", e)
        } finally {
            speechRecognizer = null
            isListening = false
        }
    }

    override fun destroy() {
        isContinuousActive = false
        mainHandler.removeCallbacks(rearmRunnable)
        mainHandler.post {
            isDestroyed = true
            Log.i(TAG, "CYPHER_VOICE: MIC_OFF")
            Log.i(TAG, "CYPHER_VOICE: LISTENING_ENDED (destroyed)")
            safeDestroy()
            _rmsLevel.value = 0f
            _partialText.value = ""
            _state.value = VoiceState.IDLE
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (isDestroyed) return
            Log.d(TAG, "CYPHER_VOICE: onReadyForSpeech - mic actively capturing")
            isListening = true
        }

        override fun onBeginningOfSpeech() {
            if (isDestroyed) return
            Log.d(TAG, "CYPHER_VOICE: onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (isDestroyed) return
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _rmsLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            if (isDestroyed) return
            Log.i(TAG, "CYPHER_VOICE: LISTENING_ENDED (end of speech)")
            isListening = false
            _rmsLevel.value = 0f
        }

        override fun onError(errorCode: Int) {
            isListening = false
            _rmsLevel.value = 0f
            if (isDestroyed) return

            Log.w(TAG, "CYPHER_VOICE: RECOGNIZER_ERROR (code=$errorCode)")

            when (errorCode) {
                // Routine silence or end-of-turn in continuous listening -> seamlessly re-arm
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    if (isContinuousActive) {
                        rearmContinuously(delayMs = 50L)
                    }
                }

                // Recognizer busy -> cancel active and re-arm with brief settling delay
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    try { speechRecognizer?.cancel() } catch (_: Exception) {}
                    if (isContinuousActive) {
                        rearmContinuously(delayMs = 250L)
                    }
                }

                // Server or client disconnection -> recreate recognizer and re-arm
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
                SpeechRecognizer.ERROR_CLIENT -> {
                    safeDestroy()
                    if (isContinuousActive) {
                        rearmContinuously(delayMs = 200L)
                    }
                }

                // Insufficient permissions -> emit user-facing notification
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    val msg = "Microphone permission is required."
                    Log.e(TAG, "CYPHER_VOICE: $msg (code=$errorCode)")
                    _state.value = VoiceState.ERROR
                    scope.launch { _errorEvents.emit(msg) }
                }

                // Network error -> Continue listening with backoff
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    if (isContinuousActive) {
                        rearmContinuously(delayMs = 500L)
                    }
                }

                else -> {
                    try { speechRecognizer?.cancel() } catch (_: Exception) {}
                    if (isContinuousActive) {
                        rearmContinuously(delayMs = 200L)
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            _rmsLevel.value = 0f
            if (isDestroyed) return

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim().orEmpty()
            Log.i(TAG, "CYPHER_VOICE: RECOGNIZER_RESULTS (candidates=${matches?.size}) = \"$text\"")

            _recognizedText.value = text
            _partialText.value = text

            if (text.isNotBlank()) {
                scope.launch { _recognizedEvents.emit(text) }
            } else {
                if (isContinuousActive) {
                    rearmContinuously(delayMs = 50L)
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (isDestroyed) return
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotBlank()) {
                _partialText.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
