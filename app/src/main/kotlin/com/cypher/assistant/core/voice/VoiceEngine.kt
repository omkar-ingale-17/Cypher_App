package com.cypher.assistant.core.voice

import android.util.Log
import com.cypher.assistant.core.command.CommandIntentType
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CYPHER_VOICE"

/**
 * Core Voice Assistant Orchestrator for Cypher AI.
 */
@Singleton
class VoiceEngine @Inject constructor(
    private val sttEngine: SpeechRecognizerEngine,
    private val ttsEngine: TTSEngine,
    private val wakeDetector: WakeWordDetector,
    private val intentEngine: CommandIntentEngine,
    private val commandRouter: CommandRouter,
    private val historyRepository: CommandHistoryRepository
) {
    private var engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private val _errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    val rmsLevel: StateFlow<Float> = sttEngine.rmsLevel
    val availableVoices: StateFlow<List<VoiceInfo>> = ttsEngine.availableVoices
    val availableLanguages: StateFlow<List<Locale>> = ttsEngine.availableLanguages

    private var activeLocale: Locale = Locale.getDefault()
    private var processingJob: Job? = null
    private var watchdogJob: Job? = null
    private var isShutdown: Boolean = false

    @Volatile
    private var lastSttActivityTime: Long = System.currentTimeMillis()

    init {
        observeSttEvents()
        startMicWatchdog()
    }

    /**
     * Start a periodic watchdog that checks if the recognizer stalled
     * while in wake-word or command listening mode.
     */
    private fun startMicWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = engineScope.launch {
            while (isActive) {
                delay(20_000L)
                if (isShutdown) break

                val state = _voiceState.value
                val isListeningState = state == VoiceState.LISTENING_FOR_WAKE_WORD ||
                        state == VoiceState.LISTENING_FOR_COMMAND
                val silentDuration = System.currentTimeMillis() - lastSttActivityTime

                if (isListeningState && silentDuration > 35_000L && state != VoiceState.SPEAKING && state != VoiceState.PROCESSING) {
                    Log.w(TAG, "CYPHER_VOICE: Watchdog detected silent stall (${silentDuration / 1000}s) -> re-arming recognizer")
                    lastSttActivityTime = System.currentTimeMillis()
                    sttEngine.startListening(locale = activeLocale, preferOffline = true)
                }
            }
        }
    }

    private fun observeSttEvents() {
        engineScope.launch {
            sttEngine.recognizedEvents.collect { text ->
                lastSttActivityTime = System.currentTimeMillis()
                if (isShutdown) return@collect

                val currentState = _voiceState.value
                if (currentState == VoiceState.SPEAKING || currentState == VoiceState.PROCESSING) {
                    Log.d(TAG, "CYPHER_VOICE: Dropping STT result during $currentState: \"$text\"")
                    return@collect
                }

                _currentTranscript.value = text
                handleFinalRecognizedSpeech(text)
            }
        }

        engineScope.launch {
            sttEngine.partialText.collect { partial ->
                lastSttActivityTime = System.currentTimeMillis()
                if (isShutdown) return@collect
                val state = _voiceState.value
                if (state != VoiceState.SPEAKING && state != VoiceState.PROCESSING) {
                    _currentTranscript.value = partial
                }
            }
        }

        engineScope.launch {
            sttEngine.errorEvents.collect { errorMsg ->
                lastSttActivityTime = System.currentTimeMillis()
                if (isShutdown) return@collect
                Log.w(TAG, "CYPHER_VOICE: STT error received: $errorMsg")
                _errorMessages.emit(errorMsg)
            }
        }

        engineScope.launch {
            sttEngine.silenceTimeoutEvents.collect {
                lastSttActivityTime = System.currentTimeMillis()
            }
        }
    }

    /**
     * Start wake-word or command listening.
     */
    fun startListening(requireWakePhrase: Boolean = true, continuous: Boolean = true) {
        if (isShutdown) {
            isShutdown = false
            if (!engineScope.isActive) {
                engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                observeSttEvents()
                startMicWatchdog()
            }
        }

        lastSttActivityTime = System.currentTimeMillis()
        _voiceState.value = if (requireWakePhrase) {
            VoiceState.LISTENING_FOR_WAKE_WORD
        } else {
            VoiceState.LISTENING_FOR_COMMAND
        }
        Log.i(TAG, "CYPHER_VOICE: LISTENING_STARTED (requireWakePhrase=$requireWakePhrase, continuous=$continuous)")
        sttEngine.startListening(locale = activeLocale, preferOffline = true)
    }

    /**
     * Ensures listening is active if Cypher is in a listening state.
     */
    fun ensureListeningActive() {
        if (isShutdown) return
        val state = _voiceState.value
        if (state == VoiceState.LISTENING_FOR_WAKE_WORD || state == VoiceState.IDLE) {
            Log.d(TAG, "CYPHER_VOICE: ensureListeningActive -> re-arming wake-word listening")
            startListening(requireWakePhrase = true)
        } else if (state == VoiceState.LISTENING_FOR_COMMAND) {
            Log.d(TAG, "CYPHER_VOICE: ensureListeningActive -> re-arming command listening")
            startListening(requireWakePhrase = false)
        }
    }

    private fun startCommandListening() {
        _voiceState.value = VoiceState.LISTENING_FOR_COMMAND
        lastSttActivityTime = System.currentTimeMillis()
        Log.i(TAG, "CYPHER_VOICE: Starting direct command listening (Stage 2)")
        sttEngine.startListening(locale = activeLocale, preferOffline = true)
    }

    fun stopListening() {
        sttEngine.stopListening()
        val state = _voiceState.value
        if (state == VoiceState.LISTENING_FOR_WAKE_WORD ||
            state == VoiceState.LISTENING_FOR_COMMAND
        ) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun cancel() {
        processingJob?.cancel()
        sttEngine.cancel()
        ttsEngine.stop()
        _voiceState.value = VoiceState.IDLE
    }

    fun processTextInput(text: String) {
        if (text.isBlank() || isShutdown) return
        _currentTranscript.value = text
        _voiceState.value = VoiceState.PROCESSING
        executeCommandPipeline(text, CommandSource.TEXT)
    }

    private fun handleFinalRecognizedSpeech(rawSpeech: String) {
        if (isShutdown) return
        Log.d(TAG, "CYPHER_VOICE: Final transcript = \"$rawSpeech\" (state=${_voiceState.value})")
        when (_voiceState.value) {
            VoiceState.LISTENING_FOR_WAKE_WORD -> {
                val result = wakeDetector.process(rawSpeech, requireWakePhrase = true)
                when (result) {
                    is WakePhraseResult.WakeWithCommand -> {
                        Log.i(TAG, "CYPHER_VOICE: Wake word detected = ${result.wakeWord}, command = \"${result.commandPayload}\"")
                        _voiceState.value = VoiceState.WAKE_WORD_DETECTED
                        executeCommandPipeline(result.commandPayload, CommandSource.VOICE)
                    }
                    is WakePhraseResult.WakeOnly -> {
                        Log.i(TAG, "CYPHER_VOICE: Standalone wake word = ${result.wakeWord}. Prompting...")
                        _voiceState.value = VoiceState.WAKE_WORD_DETECTED
                        engineScope.launch {
                            val prompt = "Yes?"
                            _lastResponse.value = prompt
                            speakResponse(prompt)
                            if (!isShutdown) {
                                startCommandListening()
                            }
                        }
                    }
                    is WakePhraseResult.DirectCommand,
                    is WakePhraseResult.None -> {
                        Log.d(TAG, "CYPHER_VOICE: Non-wake speech in standby: \"$rawSpeech\" -> continuing wake-word listening")
                        startListening(requireWakePhrase = true)
                    }
                }
            }

            VoiceState.LISTENING_FOR_COMMAND -> {
                val result = wakeDetector.process(rawSpeech, requireWakePhrase = false)
                val command = when (result) {
                    is WakePhraseResult.WakeWithCommand -> result.commandPayload
                    is WakePhraseResult.DirectCommand   -> result.commandPayload
                    is WakePhraseResult.WakeOnly        -> ""
                    is WakePhraseResult.None            -> rawSpeech
                }
                if (command.isNotBlank()) {
                    Log.i(TAG, "CYPHER_VOICE: Command detected = \"$command\"")
                    executeCommandPipeline(command, CommandSource.VOICE)
                } else {
                    startListening(requireWakePhrase = true)
                }
            }

            else -> {
                Log.d(TAG, "CYPHER_VOICE: Speech received in state ${_voiceState.value} -> continuing listening")
                startListening(requireWakePhrase = true)
            }
        }
    }

    private fun executeCommandPipeline(commandText: String, source: CommandSource) {
        if (isShutdown) return
        processingJob?.cancel()
        processingJob = engineScope.launch {
            try {
                _voiceState.value = VoiceState.PROCESSING

                // 1. Pre-process & Parse Intent
                val normalized = intentEngine.preProcessText(commandText)
                val intent = intentEngine.parse(commandText, source)
                val extractedAppName = intent.parameters["app_name"] ?: "N/A"

                Log.i("CYPHER_COMMAND", """
                    ======================================================
                    Recognized text: "$commandText"
                    Normalized text: "$normalized"
                    Detected intent: ${intent.intentType}
                    Extracted app name: $extractedAppName
                    Parameters: ${intent.parameters}
                    ======================================================
                """.trimIndent())

                // 2. Route & Execute Action
                val result = commandRouter.route(intent)
                _lastResponse.value = result.message
                Log.i("CYPHER_COMMAND", "Executed action: ${intent.intentType} -> \"${result.message}\" (success=${result.success})")

                // 3. Persist in database
                historyRepository.record(intent, result)

                // 4. TTS -> speaks response while microphone is paused (MIC OFF -> TTS -> MIC ON)
                speakResponse(result.message)

                // 5. Return to continuous wake-word listening
                if (isShutdown) return@launch
                Log.i(TAG, "CYPHER_VOICE: TTS completed -> returning to continuous wake-word listening")
                delay(200L)
                if (!isShutdown) {
                    startListening(requireWakePhrase = true)
                }

            } catch (e: Exception) {
                if (isShutdown) return@launch
                Log.e(TAG, "CYPHER_VOICE: Error in command pipeline", e)
                _voiceState.value = VoiceState.ERROR
                _errorMessages.emit("An error occurred.")
                delay(400L)
                if (!isShutdown) {
                    startListening(requireWakePhrase = true)
                }
            }
        }
    }

    /**
     * Speaks response via TTS while microphone is paused.
     * Flow: MIC OFF -> TTS SPEAKING -> TTS DONE -> MIC ON
     */
    private suspend fun speakResponse(text: String) {
        if (text.isBlank() || isShutdown) return

        _voiceState.value = VoiceState.SPEAKING
        sttEngine.cancel()
        delay(200L)

        Log.d(TAG, "CYPHER_TTS: Speaking = \"${text.take(80)}\"")
        try {
            withTimeoutOrNull(35_000L) {
                ttsEngine.speak(text)
            } ?: Log.w(TAG, "CYPHER_TTS: TTS timed out after 35s for \"${text.take(60)}\"")
        } catch (e: Exception) {
            Log.e(TAG, "CYPHER_TTS: TTS error", e)
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
        isShutdown = true
        watchdogJob?.cancel()
        processingJob?.cancel()
        sttEngine.destroy()
        ttsEngine.shutdown()
        _voiceState.value = VoiceState.IDLE
        Log.i(TAG, "CYPHER_VOICE: VoiceEngine shutdown completed")
    }
}
