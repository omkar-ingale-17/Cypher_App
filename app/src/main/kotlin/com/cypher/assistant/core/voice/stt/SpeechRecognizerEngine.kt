package com.cypher.assistant.core.voice.stt

import com.cypher.assistant.core.voice.VoiceState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Pluggable contract for Speech-to-Text engines (e.g. Android SpeechRecognizer,
 * Whisper, Cloud STT).
 */
interface SpeechRecognizerEngine {
    /** Current state of the recognizer (IDLE, LISTENING, PROCESSING, etc.). */
    val state: StateFlow<VoiceState>

    /** Real-time microphone audio level normalized between 0.0f and 1.0f. */
    val rmsLevel: StateFlow<Float>

    /** Final recognized text output from the current utterance. */
    val recognizedText: StateFlow<String>

    /** Live partial transcript updated as user speaks. */
    val partialText: StateFlow<String>

    /** Shared flow of one-time error descriptions. */
    val errorEvents: SharedFlow<String>

    /** Checks if speech recognition service is available on this device. */
    fun isAvailable(): Boolean

    /**
     * Start listening for voice input in the specified locale.
     * @param locale Speech recognition locale (defaults to device default).
     * @param preferOffline Prefer on-device recognition if supported by the engine.
     */
    fun startListening(locale: Locale = Locale.getDefault(), preferOffline: Boolean = false)

    /** Stop listening and finalize current speech stream. */
    fun stopListening()

    /** Cancel active recognition without producing results. */
    fun cancel()

    /** Clean up all resources and audio handles. */
    fun destroy()
}
