package com.cypher.assistant.core.voice.stt

import com.cypher.assistant.core.voice.VoiceState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Contract for speech-to-text engines in Cypher.
 */
interface SpeechRecognizerEngine {
    val state: StateFlow<VoiceState>
    val recognizedText: StateFlow<String>
    val partialText: StateFlow<String>
    val rmsLevel: StateFlow<Float>

    /** Emits non-empty final speech recognition results. */
    val recognizedEvents: SharedFlow<String>

    /** Emits user-facing recoverable error messages. */
    val errorEvents: SharedFlow<String>

    /** Emits notifications on silence / timeout / recoverable no-match for continuous loop resumption. */
    val silenceTimeoutEvents: SharedFlow<Unit>

    fun startListening(locale: Locale = Locale.getDefault(), preferOffline: Boolean = false)
    fun stopListening()
    fun cancel()
    fun destroy()
    fun isAvailable(): Boolean
}
