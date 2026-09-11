package com.cypher.assistant.core.voice.tts

import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID

/**
 * Pluggable contract for Text-To-Speech engines (e.g. Android TTS, ElevenLabs, Cloud TTS).
 */
interface TTSEngine {
    /** True when TTS engine is successfully initialized and ready for speech. */
    val isReady: StateFlow<Boolean>

    /** True when TTS is currently synthesizing or speaking. */
    val isSpeaking: StateFlow<Boolean>

    /** List of voices installed and supported by the active TTS engine. */
    val availableVoices: StateFlow<List<VoiceInfo>>

    /** List of languages supported by the active TTS engine. */
    val availableLanguages: StateFlow<List<Locale>>

    /**
     * Synthesizes and speaks [text].
     * Suspends until utterance playback is finished or cancelled.
     *
     * @param text Text string to speak aloud.
     * @param queueMode [TextToSpeech.QUEUE_FLUSH] to interrupt, [TextToSpeech.QUEUE_ADD] to append.
     * @param utteranceId Unique ID tracking this speech segment.
     */
    suspend fun speak(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        utteranceId: String = UUID.randomUUID().toString()
    ): Result<Unit>

    /** Stop all currently playing and queued speech immediately. */
    fun stop()

    /** Configure the target language/locale for speech output. */
    fun setLanguage(locale: Locale): Boolean

    /** Set a specific installed voice by identifier. */
    fun setVoice(voiceName: String): Boolean

    /** Set speech rate factor (0.5f to 2.0f, where 1.0f is normal). */
    fun setSpeechRate(rate: Float)

    /** Set speech pitch factor (0.5f to 2.0f, where 1.0f is normal). */
    fun setPitch(pitch: Float)

    /** Release engine and audio track resources. */
    fun shutdown()
}
