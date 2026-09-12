package com.cypher.assistant.core.voice

import android.util.Log
import com.cypher.assistant.core.command.CommandRouter
import com.cypher.assistant.core.command.CommandSource
import com.cypher.assistant.core.intent.CommandIntentEngine
import com.cypher.assistant.core.voice.stt.SpeechRecognizerEngine
import com.cypher.assistant.core.voice.tts.TTSEngine
import com.cypher.assistant.core.voice.tts.VoiceInfo
import com.cypher.assistant.core.voice.wake.WakePhraseResult
import com.cypher.assistant.core.voice.wake.WakeWordDetector
import com.cypher.assistant.data.preferences.UserPreferencesDataStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CypherVoiceEngine"

/**
 * Production-grade Two-Stage Voice Orchestrator for Cypher AI:
 *
 * STAGE 1: Wake-Word Listening (LISTENING_FOR_WAKE_WORD)
 * - Listens continuously for official wake words (cypher, cipher, jan, jaan, jann, baby)
 * - Immediate partial result detection latching
 * - Non-fatal automatic recovery on silence/timeouts
 *
 * STAGE 2: Command Listening & Execution (LISTENING_FOR_COMMAND -> PROCESSING -> SPEAKING)
 * - Captures intent, routes to handler, persists in Room DB, speaks response via TTS
 * - Seamlessly transitions back to Stage 1 upon speech completion
 *
 * MANUAL MODE:
 * - Direct tap on microphone bypasses Stage 1 and immediately initiates Stage 2
 */
@Singleton
class VoiceEngine @Inject constructor(
    private val sttEngine: SpeechRecognizerEngine,
    private val ttsEngine: TTSEngine,
    private val intentEngine: CommandIntentEngine,
    private val commandRouter: CommandRouter,
    private val historyRepository: CommandHistoryRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val wakeDetector = WakeWordDetector()

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private val _errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 3)
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    val rmsLevel: StateFlow<Float> = sttEngine.rmsLevel
    val availableVoices: StateFlow<List<VoiceInfo>> = ttsEngine.availableVoices
    val availableLanguages: StateFlow<List<Locale>> = ttsEngine.availableLanguages

    private var activeLocale: Locale = Locale.getDefault()
    private var isContinuousListeningMode: Boolean = false
    private var isWakeWordModeActive: Boolean = false

    // Latch to prevent duplicate activations within the same utterance
    private val isWakeWordTriggered = AtomicBoolean(false)

    private var loopRestartJob: Job? = null
    private var processingJob: Job? = null

    init {
        observeSttEvents()
    }

    private fun observeSttEvents() {
        // 1. Observe partial results for low-latency wake-word triggers and live UI transcript
        engineScope.launch {
            sttEngine.partialText.collect { partial ->
                if (partial.isBlank()) return@collect

                when (_voiceState.value) {
                    VoiceState.LISTENING_FOR_WAKE_WORD -> {
                        _currentTranscript.value = partial
                        handlePartialWakeWord(partial)
                    }
                    VoiceState.LISTENING_FOR_COMMAND -> {
                        _currentTranscript.value = partial
                    }
                    else -> Unit
                }
            }
        }

        // 2. Observe final recognized speech events
        engineScope.launch {
            sttEngine.recognizedEvents.collect { finalSpeech ->
                if (finalSpeech.isBlank()) return@collect
                _currentTranscript.value = finalSpeech
                handleFinalRecognizedSpeech(finalSpeech)
            }
        }

        // 3. Observe silence / recoverable timeout events for standby loop resumption
        engineScope.launch {
            sttEngine.silenceTimeoutEvents.collect {
                handleSilenceOrTimeout()
            }
        }

        // 4. Forward hard error notifications (e.g. missing permissions, network failures)
        engineScope.launch {
            sttEngine.errorEvents.collect { err ->
                _errorMessages.emit(err)
            }
        }
    }

    /**
     * Start Stage 1 (Wake-Word Listening) or continuous listening.
     */
    fun startListening(continuous: Boolean = false, requireWakePhrase: Boolean = true) {
        isContinuousListeningMode = continuous
        isWakeWordModeActive = requireWakePhrase
        isWakeWordTriggered.set(false)
        _currentTranscript.value = ""

        if (ttsEngine.isSpeaking.value) {
            ttsEngine.stop()
        }

        if (requireWakePhrase) {
            _voiceState.value = VoiceState.LISTENING_FOR_WAKE_WORD
            Log.d(TAG, "CYPHER_VOICE: Starting STAGE 1 (Listening for wake word)")
        } else {
            _voiceState.value = VoiceState.LISTENING_FOR_COMMAND
            Log.d(TAG, "CYPHER_VOICE: Starting STAGE 2 directly (Manual command mode)")
        }

        sttEngine.startListening(locale = activeLocale)
    }

    /**
     * Start Stage 2 (Command Listening directly, e.g. after mic tap or wake-word acknowledgment).
     */
    fun startCommandListening() {
        isWakeWordModeActive = false
        isWakeWordTriggered.set(false)
        _currentTranscript.value = ""
        _voiceState.value = VoiceState.LISTENING_FOR_COMMAND
        Log.d(TAG, "CYPHER_VOICE: Starting STAGE 2 (Listening for command)")

        if (ttsEngine.isSpeaking.value) {
            ttsEngine.stop()
        }

        sttEngine.startListening(locale = activeLocale)
    }

    /**
     * Stop active voice capture and return to IDLE.
     */
    fun stopListening() {
        loopRestartJob?.cancel()
        sttEngine.stopListening()
        if (_voiceState.value == VoiceState.LISTENING_FOR_WAKE_WORD ||
            _voiceState.value == VoiceState.LISTENING_FOR_COMMAND
        ) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    /**
     * Cancel listening, processing, and TTS immediately.
     */
    fun cancel() {
        loopRestartJob?.cancel()
        processingJob?.cancel()
        isWakeWordTriggered.set(false)
        sttEngine.cancel()
        ttsEngine.stop()
        _voiceState.value = VoiceState.IDLE
    }

    /**
     * Process manually typed text command.
     */
    fun processTextInput(text: String) {
        if (text.isBlank()) return
        _currentTranscript.value = text
        _voiceState.value = VoiceState.PROCESSING
        executeCommandPipeline(text, CommandSource.TEXT)
    }

    /**
     * Evaluates partial results in Stage 1 to detect wake word immediately without waiting for speech silence.
     */
    private fun handlePartialWakeWord(partial: String) {
        if (isWakeWordTriggered.get()) return

        val detectedWord = wakeDetector.containsWakeWord(partial)
        if (detectedWord != null && isWakeWordTriggered.compareAndSet(false, true)) {
            Log.d(TAG, "CYPHER_VOICE: Wake word '$detectedWord' detected in partial result: '$partial'")

            // Stop Stage 1 listening session safely
            sttEngine.stopListening()

            val result = wakeDetector.process(partial, requireWakePhrase = true)
            handleWakeWordDetected(result, detectedWord)
        }
    }

    /**
     * Evaluates final recognized speech based on current voice state.
     */
    private fun handleFinalRecognizedSpeech(rawSpeech: String) {
        when (_voiceState.value) {
            VoiceState.LISTENING_FOR_WAKE_WORD -> {
                if (isWakeWordTriggered.get()) return // already triggered via partial

                val result = wakeDetector.process(rawSpeech, requireWakePhrase = true)
                when (result) {
                    is WakePhraseResult.WakeOnly -> {
                        if (isWakeWordTriggered.compareAndSet(false, true)) {
                            handleWakeWordDetected(result, result.wakeWord)
                        }
                    }
                    is WakePhraseResult.WakeWithCommand -> {
                        if (isWakeWordTriggered.compareAndSet(false, true)) {
                            handleWakeWordDetected(result, result.wakeWord)
                        }
                    }
                    is WakePhraseResult.DirectCommand,
                    is WakePhraseResult.None -> {
                        // Non-wake word in standby mode -> schedule silent standby loop restart
                        scheduleStandbyLoopRestart()
                    }
                }
            }

            VoiceState.LISTENING_FOR_COMMAND -> {
                // User spoke in command listening mode
                _voiceState.value = VoiceState.PROCESSING
                val result = wakeDetector.process(rawSpeech, requireWakePhrase = false)
                val command = when (result) {
                    is WakePhraseResult.WakeWithCommand -> result.commandPayload
                    is WakePhraseResult.DirectCommand -> result.commandPayload
                    is WakePhraseResult.WakeOnly -> ""
                    is WakePhraseResult.None -> rawSpeech
                }

                if (command.isNotBlank()) {
                    executeCommandPipeline(command, CommandSource.VOICE)
                } else {
                    // Empty command -> recover back to Stage 1 or IDLE
                    scheduleRecoveryAfterEmptyCommand()
                }
            }

            else -> Unit
        }
    }

    /**
     * Handles wake word detection trigger (Stage 1 -> Stage 2 transition or direct execution).
     */
    private fun handleWakeWordDetected(result: WakePhraseResult, wakeWord: String) {
        _voiceState.value = VoiceState.WAKE_WORD_DETECTED
        Log.d(TAG, "CYPHER_VOICE: Wake word detected = $wakeWord. Transitioning...")

        when (result) {
            is WakePhraseResult.WakeWithCommand -> {
                // "Cypher, what is the time" -> execute command directly
                executeCommandPipeline(result.commandPayload, CommandSource.VOICE)
            }
            else -> {
                // "Cypher" -> speak quick audio acknowledgment, then open command listener
                engineScope.launch {
                    val prompt = "I'm listening."
                    _lastResponse.value = prompt
                    speakResponse(prompt)
                    // Brief audio drain delay before starting command recognition
                    delay(250)
                    startCommandListening()
                }
            }
        }
    }

    /**
     * Handles silence, timeout, or recoverable speech recognition error.
     */
    private fun handleSilenceOrTimeout() {
        when (_voiceState.value) {
            VoiceState.LISTENING_FOR_WAKE_WORD -> {
                // Continue Stage 1 standby loop
                scheduleStandbyLoopRestart()
            }
            VoiceState.LISTENING_FOR_COMMAND -> {
                // User timed out while in command mode -> return to Stage 1 if wake-word enabled, else IDLE
                scheduleRecoveryAfterEmptyCommand()
            }
            else -> Unit
        }
    }

    private fun scheduleStandbyLoopRestart() {
        if (_voiceState.value != VoiceState.LISTENING_FOR_WAKE_WORD) return
        loopRestartJob?.cancel()
        loopRestartJob = engineScope.launch {
            delay(150)
            if (_voiceState.value == VoiceState.LISTENING_FOR_WAKE_WORD) {
                isWakeWordTriggered.set(false)
                Log.d(TAG, "CYPHER_VOICE: Restarting wake-word listener loop")
                sttEngine.startListening(locale = activeLocale)
            }
        }
    }

    private fun scheduleRecoveryAfterEmptyCommand() {
        engineScope.launch {
            val prefs = preferencesDataStore.userPreferences.first()
            if (prefs.wakeWordEnabled || isContinuousListeningMode) {
                delay(200)
                startListening(
                    continuous = prefs.continuousListening,
                    requireWakePhrase = true
                )
            } else {
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    /**
     * Executes the intent parsing, command routing, history persistence, and TTS playback pipeline.
     */
    private fun executeCommandPipeline(commandText: String, source: CommandSource) {
        processingJob?.cancel()
        processingJob = engineScope.launch {
            try {
                _voiceState.value = VoiceState.PROCESSING
                Log.d(TAG, "CYPHER_VOICE: Processing command = '$commandText' from $source")

                // 1. Intent Parsing
                val intent = intentEngine.parse(commandText, source)
                Log.d(TAG, "CYPHER_VOICE: Intent parsed = ${intent.intentType} (params=${intent.parameters})")

                // 2. Route & Execute
                val result = commandRouter.route(intent)
                _lastResponse.value = result.message

                // 3. Persist record in Room database
                historyRepository.record(intent, result)

                // 4. TTS Feedback
                speakResponse(result.message)

                // 5. Post-command lifecycle: return to Stage 1 wake-word standby or IDLE
                val prefs = preferencesDataStore.userPreferences.first()
                if (prefs.wakeWordEnabled || isContinuousListeningMode) {
                    delay(350)
                    startListening(
                        continuous = prefs.continuousListening,
                        requireWakePhrase = true
                    )
                } else {
                    _voiceState.value = VoiceState.IDLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "CYPHER_VOICE: Error executing command pipeline", e)
                val errMsg = "An error occurred while executing the command."
                _lastResponse.value = errMsg
                _voiceState.value = VoiceState.ERROR
                _errorMessages.emit(errMsg)
            }
        }
    }

    /**
     * Speaks response text via TTS while pausing the microphone to prevent feedback.
     */
    private suspend fun speakResponse(text: String) {
        if (text.isBlank()) return
        _voiceState.value = VoiceState.SPEAKING
        sttEngine.cancel() // Stop STT during TTS to avoid echo feedback

        try {
            withTimeoutOrNull(8000L) {
                ttsEngine.speak(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "CYPHER_VOICE: TTS playback error", e)
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
