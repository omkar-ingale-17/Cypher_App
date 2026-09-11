package com.cypher.assistant.core.voice

import android.util.Log
import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.core.command.CommandRouter
import com.cypher.assistant.core.command.CommandSource
import com.cypher.assistant.core.intent.CommandIntentEngine
import com.cypher.assistant.core.voice.stt.SpeechRecognizerEngine
import com.cypher.assistant.core.voice.tts.TTSEngine
import com.cypher.assistant.core.voice.tts.VoiceInfo
import com.cypher.assistant.core.voice.wake.WakePhraseResult
import com.cypher.assistant.core.voice.wake.WakeWordDetector
import com.cypher.assistant.data.repository.CommandHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VoiceEngine"

/**
 * Unified facade coordinating Speech-to-Text, Wake Word Detection,
 * Intent Parsing, Command Routing, and Text-to-Speech synthesis.
 */
@Singleton
class VoiceEngine @Inject constructor(
    private val sttEngine: SpeechRecognizerEngine,
    private val ttsEngine: TTSEngine,
    private val intentEngine: CommandIntentEngine,
    private val commandRouter: CommandRouter,
    private val historyRepository: CommandHistoryRepository
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val wakeDetector = WakeWordDetector()

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private val _errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    val rmsLevel: StateFlow<Float> = sttEngine.rmsLevel
    val availableVoices: StateFlow<List<VoiceInfo>> = ttsEngine.availableVoices
    val availableLanguages: StateFlow<List<Locale>> = ttsEngine.availableLanguages

    private var activeLocale: Locale = Locale.getDefault()
    private var isContinuousListeningMode: Boolean = false
    private var isWakeWordActive: Boolean = false
    private var processingJob: Job? = null

    init {
        observeSttState()
    }

    private fun observeSttState() {
        // Synchronize STT partial results with live transcript
        engineScope.launch {
            sttEngine.partialText.collect { partial ->
                if (_voiceState.value == VoiceState.LISTENING && partial.isNotBlank()) {
                    _currentTranscript.value = partial
                }
            }
        }

        // Listen for final recognized transcripts
        engineScope.launch {
            sttEngine.recognizedText.collect { finalResult ->
                if (finalResult.isNotBlank()) {
                    _currentTranscript.value = finalResult
                    handleRecognizedSpeech(finalResult)
                }
            }
        }

        // Forward STT error notifications
        engineScope.launch {
            sttEngine.errorEvents.collect { err ->
                _errorMessages.emit(err)
                _voiceState.value = VoiceState.ERROR
            }
        }
    }

    /**
     * Start listening for voice input.
     *
     * @param continuous If true, engine runs in continuous listening / wake-phrase mode.
     * @param requireWakePhrase If true, inputs require the "Cypher" prefix.
     */
    fun startListening(continuous: Boolean = false, requireWakePhrase: Boolean = false) {
        isContinuousListeningMode = continuous
        isWakeWordActive = requireWakePhrase
        _currentTranscript.value = ""
        _voiceState.value = VoiceState.LISTENING

        // Stop TTS if currently speaking
        if (ttsEngine.isSpeaking.value) {
            ttsEngine.stop()
        }

        sttEngine.startListening(locale = activeLocale)
        Log.d(TAG, "VoiceEngine listening started (continuous=$continuous, wakeWord=$requireWakePhrase)")
    }

    /**
     * Stop active voice capture.
     */
    fun stopListening() {
        sttEngine.stopListening()
        if (_voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    /**
     * Cancel listening or speaking immediately.
     */
    fun cancel() {
        processingJob?.cancel()
        sttEngine.cancel()
        ttsEngine.stop()
        _voiceState.value = VoiceState.IDLE
    }

    /**
     * Manually submit a text command (e.g. from keyboard input).
     */
    fun processTextInput(text: String) {
        if (text.isBlank()) return
        _currentTranscript.value = text
        _voiceState.value = VoiceState.PROCESSING

        executeCommandPipeline(text, CommandSource.TEXT)
    }

    private fun handleRecognizedSpeech(rawSpeech: String) {
        _voiceState.value = VoiceState.PROCESSING

        // Evaluate wake phrase
        when (val result = wakeDetector.process(rawSpeech, requireWakePhrase = isWakeWordActive)) {
            is WakePhraseResult.WakeOnly -> {
                // User said "Cypher" -> Acknowledge and immediately listen for the next command
                engineScope.launch {
                    val prompt = "I'm listening."
                    _lastResponse.value = prompt
                    speakResponse(prompt)
                    // After speaking acknowledgement, open mic for the user's command
                    startListening(continuous = isContinuousListeningMode, requireWakePhrase = false)
                }
            }

            is WakePhraseResult.WakeWithCommand -> {
                // User said "Cypher, open WhatsApp" -> Execute the command payload
                executeCommandPipeline(result.commandPayload, CommandSource.VOICE)
            }

            is WakePhraseResult.DirectCommand -> {
                // Direct command without wake word prefix
                executeCommandPipeline(result.commandPayload, CommandSource.VOICE)
            }

            is WakePhraseResult.None -> {
                // Ignored in continuous mode without wake word
                if (isContinuousListeningMode) {
                    startListening(continuous = true, requireWakePhrase = isWakeWordActive)
                } else {
                    _voiceState.value = VoiceState.IDLE
                }
            }
        }
    }

    private fun executeCommandPipeline(commandText: String, source: CommandSource) {
        processingJob?.cancel()
        processingJob = engineScope.launch {
            try {
                // 1. Normalized text → Intent Engine
                val intent = intentEngine.parse(commandText, source)
                Log.d(TAG, "Parsed intent: ${intent.intentType} params=${intent.parameters}")

                // 2. Intent → Command Router
                val result = commandRouter.route(intent)
                _lastResponse.value = result.message

                // 3. Persist record to Room database
                historyRepository.record(intent, result)

                // 4. Speak response via TTS
                speakResponse(result.message)

                // 5. If in continuous listening mode, restart listening loop
                if (isContinuousListeningMode) {
                    startListening(continuous = true, requireWakePhrase = isWakeWordActive)
                } else {
                    _voiceState.value = VoiceState.IDLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing command pipeline", e)
                val errMsg = "An error occurred while executing the command."
                _lastResponse.value = errMsg
                _voiceState.value = VoiceState.ERROR
                _errorMessages.emit(errMsg)
            }
        }
    }

    private suspend fun speakResponse(text: String) {
        if (text.isBlank()) return
        _voiceState.value = VoiceState.SPEAKING
        try {
            ttsEngine.speak(text)
        } catch (e: Exception) {
            Log.e(TAG, "TTS playback error", e)
        }
    }

    fun setLanguage(locale: Locale) {
        activeLocale = locale
        ttsEngine.setLanguage(locale)
    }

    fun setVoice(voiceName: String) {
        ttsEngine.setVoice(voiceName)
    }

    fun setSpeechRate(rate: Float) {
        ttsEngine.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        ttsEngine.setPitch(pitch)
    }

    fun shutdown() {
        cancel()
        sttEngine.destroy()
        ttsEngine.shutdown()
    }
}
