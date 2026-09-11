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

private const val TAG = "AndroidSTTEngine"

@Singleton
class AndroidSpeechRecognizerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechRecognizerEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(VoiceState.IDLE)
    override val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    override val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    override val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _partialText = MutableStateFlow("")
    override val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun startListening(locale: Locale, preferOffline: Boolean) {
        mainHandler.post {
            try {
                if (!isAvailable()) {
                    val msg = "Speech recognition is not available on this device."
                    Log.e(TAG, msg)
                    _state.value = VoiceState.ERROR
                    scope.launch { _errorEvents.emit(msg) }
                    return@post
                }

                // Clean previous recognizer instance if active
                speechRecognizer?.let {
                    it.cancel()
                    it.destroy()
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
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
                _state.value = VoiceState.LISTENING

                speechRecognizer?.startListening(intent)
                Log.d(TAG, "SpeechRecognizer started listening with locale ${locale.toLanguageTag()}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition", e)
                _state.value = VoiceState.ERROR
                scope.launch { _errorEvents.emit("Failed to initialize microphone: ${e.localizedMessage}") }
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                _state.value = VoiceState.PROCESSING
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SpeechRecognizer", e)
            }
        }
    }

    override fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                _state.value = VoiceState.IDLE
                _rmsLevel.value = 0f
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling SpeechRecognizer", e)
            }
        }
    }

    override fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                _state.value = VoiceState.IDLE
                _rmsLevel.value = 0f
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying SpeechRecognizer", e)
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
            _state.value = VoiceState.LISTENING
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
            _state.value = VoiceState.LISTENING
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize typical RMS range [-2.0dB .. 10.0dB] to [0.0 .. 1.0]
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _rmsLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            _rmsLevel.value = 0f
            _state.value = VoiceState.PROCESSING
        }

        override fun onError(errorCode: Int) {
            _rmsLevel.value = 0f
            val (message, isFatal) = parseSpeechError(errorCode)
            Log.w(TAG, "Speech recognition error: $errorCode -> $message")

            if (isFatal) {
                _state.value = VoiceState.ERROR
                scope.launch { _errorEvents.emit(message) }
            } else {
                // Non-fatal errors (e.g. silence or no match) return cleanly to IDLE
                _state.value = VoiceState.IDLE
            }
        }

        override fun onResults(results: Bundle?) {
            _rmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim().orEmpty()
            Log.d(TAG, "onResults: $text (candidates: ${matches?.size})")

            _recognizedText.value = text
            _partialText.value = text
            if (text.isNotBlank()) {
                _state.value = VoiceState.PROCESSING
            } else {
                _state.value = VoiceState.IDLE
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotBlank()) {
                _partialText.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun parseSpeechError(code: Int): Pair<String, Boolean> {
        return when (code) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error" to true
            SpeechRecognizer.ERROR_CLIENT -> "Client speech recognition error" to false
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required" to true
            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition" to true
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timed out" to true
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap to try again." to false
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please retry." to false
            SpeechRecognizer.ERROR_SERVER -> "Server recognition error" to true
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected" to false
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Selected language not supported for voice recognition" to true
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Selected language unavailable offline" to true
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Recognition server disconnected" to true
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many recognition requests" to true
            else -> "Speech recognition error ($code)" to false
        }
    }
}
