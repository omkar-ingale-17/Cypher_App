package com.cypher.assistant.core.voice.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private const val TAG = "AndroidTTSEngine"

@Singleton
class AndroidTTSEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TTSEngine, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    override val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<Locale>>(emptyList())
    override val availableLanguages: StateFlow<List<Locale>> = _availableLanguages.asStateFlow()

    // Map of active utterance completions
    private val pendingUtterances = ConcurrentHashMap<String, Continuation<Result<Unit>>>()

    init {
        initialize()
    }

    private fun initialize() {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate Android TextToSpeech", e)
            _isReady.value = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            Log.i(TAG, "Android TextToSpeech initialized successfully")

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    utteranceId?.let { id ->
                        pendingUtterances.remove(id)?.resume(Result.success(Unit))
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    utteranceId?.let { id ->
                        pendingUtterances.remove(id)?.resume(
                            Result.failure(RuntimeException("TTS synthesis error on utterance $id"))
                        )
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    Log.w(TAG, "TTS utterance $utteranceId failed with code $errorCode")
                    utteranceId?.let { id ->
                        pendingUtterances.remove(id)?.resume(
                            Result.failure(RuntimeException("TTS error ($errorCode)"))
                        )
                    }
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    _isSpeaking.value = false
                    utteranceId?.let { id ->
                        pendingUtterances.remove(id)?.resume(Result.success(Unit))
                    }
                }
            })

            refreshVoicesAndLanguages()
            _isReady.value = true
        } else {
            Log.e(TAG, "TextToSpeech init failed with status: $status")
            _isReady.value = false
        }
    }

    private fun refreshVoicesAndLanguages() {
        val engine = tts ?: return
        try {
            val voices = engine.voices ?: emptySet()
            val mapped = voices.map { voice ->
                val isFemale = detectVoiceGender(voice)
                val display = buildVoiceDisplayName(voice, isFemale)
                VoiceInfo(
                    name = voice.name,
                    displayName = display,
                    locale = voice.locale,
                    isFemale = isFemale,
                    quality = voice.quality,
                    latency = voice.latency,
                    requiresNetwork = voice.isNetworkConnectionRequired
                )
            }.sortedWith(compareBy({ it.locale.displayName }, { it.displayName }))

            _availableVoices.value = mapped

            val languages = mapped.map { it.locale }.distinctBy { it.language }
            _availableLanguages.value = if (languages.isNotEmpty()) languages else listOf(Locale.getDefault())
        } catch (e: Exception) {
            Log.w(TAG, "Could not enumerate voices from TTS engine", e)
        }
    }

    private fun detectVoiceGender(voice: Voice): Boolean? {
        val nameLower = voice.name.lowercase(Locale.ROOT)
        val features = voice.features?.map { it.lowercase(Locale.ROOT) } ?: emptyList()
        return when {
            nameLower.contains("female") || nameLower.contains("woman") || features.any { it.contains("female") } -> true
            nameLower.contains("male") || nameLower.contains("man") || features.any { it.contains("male") } -> false
            // Google TTS voice heuristics e.g., "en-us-x-sfg#female" or "en-us-x-iol#male"
            nameLower.contains("-fem-") || nameLower.contains("-f-") -> true
            nameLower.contains("-mal-") || nameLower.contains("-m-") -> false
            else -> null
        }
    }

    private fun buildVoiceDisplayName(voice: Voice, isFemale: Boolean?): String {
        val genderStr = when (isFemale) {
            true -> "Female"
            false -> "Male"
            null -> "Neutral"
        }
        val localeName = voice.locale.displayLanguage
        val shortName = voice.name.substringAfterLast("-", voice.name).take(12)
        val networkTag = if (voice.isNetworkConnectionRequired) " (Cloud)" else " (Local)"
        return "$localeName * $genderStr $shortName$networkTag"
    }

    override suspend fun speak(
        text: String,
        queueMode: Int,
        utteranceId: String
    ): Result<Unit> {
        val engine = tts
        if (engine == null || !_isReady.value) {
            return Result.failure(IllegalStateException("TTS Engine is not initialized or ready"))
        }

        if (text.isBlank()) return Result.success(Unit)

        return suspendCancellableCoroutine { continuation ->
            pendingUtterances[utteranceId] = continuation
            continuation.invokeOnCancellation {
                pendingUtterances.remove(utteranceId)
                stop()
            }

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            val result = engine.speak(text, queueMode, params, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                pendingUtterances.remove(utteranceId)
                _isSpeaking.value = false
                continuation.resume(Result.failure(RuntimeException("TTS speak returned error code $result")))
            }
        }
    }

    override fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
            pendingUtterances.values.forEach { it.resume(Result.success(Unit)) }
            pendingUtterances.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    override fun setLanguage(locale: Locale): Boolean {
        val engine = tts ?: return false
        return try {
            val result = engine.setLanguage(locale)
            result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        } catch (e: Exception) {
            Log.e(TAG, "Error setting TTS language to $locale", e)
            false
        }
    }

    override fun setVoice(voiceName: String): Boolean {
        val engine = tts ?: return false
        return try {
            val voice = engine.voices?.firstOrNull { it.name == voiceName }
            if (voice != null) {
                engine.voice = voice
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting TTS voice to $voiceName", e)
            false
        }
    }

    override fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.5f))
    }

    override fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    override fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            _isReady.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error during TTS shutdown", e)
        }
    }
}
