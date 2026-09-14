package com.cypher.assistant.core.voice.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Robust Android TTS engine with self-healing, lazy re-initialization,
 * asynchronous readiness queuing, and full service-lifecycle compatibility.
 */
@Singleton
class AndroidTTSEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TTSEngine, TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "CYPHER_TTS"
    }

    private var tts: TextToSpeech? = null
    private var isInitializing = false

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    override val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<Locale>>(emptyList())
    override val availableLanguages: StateFlow<List<Locale>> = _availableLanguages.asStateFlow()

    // Pending coroutine continuations keyed by utteranceId
    private val pendingUtterances = ConcurrentHashMap<String, Continuation<Result<Unit>>>()

    private var configuredLocale: Locale = Locale.getDefault()
    private var configuredVoiceName: String? = null
    private var configuredSpeechRate: Float = 1.0f
    private var configuredPitch: Float = 1.0f

    init {
        Log.i(TAG, "CYPHER_TTS: AndroidTTSEngine constructed -> initiating initial setup")
        initialize()
    }

    /**
     * Initializes or re-initializes the underlying TextToSpeech engine.
     * Safe to call multiple times concurrently.
     */
    fun initialize() {
        synchronized(this) {
            if (tts != null && _isReady.value) {
                Log.d(TAG, "CYPHER_TTS: initialize() -> engine already ready")
                return
            }
            if (isInitializing) {
                Log.d(TAG, "CYPHER_TTS: initialize() -> initialization already in progress")
                return
            }
            isInitializing = true
        }

        try {
            Log.i(TAG, "CYPHER_TTS: TTS_INIT_START")
            _isReady.value = false
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "CYPHER_TTS: TTS_INIT_FAILED - Exception creating TextToSpeech", e)
            synchronized(this) {
                isInitializing = false
                tts = null
            }
            _isReady.value = false
        }
    }

    override fun onInit(status: Int) {
        synchronized(this) {
            isInitializing = false
        }

        if (status == TextToSpeech.SUCCESS) {
            Log.i(TAG, "CYPHER_TTS: TTS_INIT_SUCCESS")
            val engine = tts ?: run {
                Log.e(TAG, "CYPHER_TTS: TTS_INIT_FAILED - tts is null after SUCCESS status")
                _isReady.value = false
                return
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            engine.setAudioAttributes(audioAttributes)

            val langResult = engine.setLanguage(configuredLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "CYPHER_TTS: Preferred locale $configuredLocale not available, using default")
            }

            engine.setSpeechRate(configuredSpeechRate)
            engine.setPitch(configuredPitch)
            configuredVoiceName?.let { setVoice(it) }

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.i(TAG, "CYPHER_TTS: TTS_STARTED (utteranceId=$utteranceId)")
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    Log.i(TAG, "CYPHER_TTS: TTS_DONE (utteranceId=$utteranceId)")
                    _isSpeaking.value = false
                    resumePending(utteranceId, Result.success(Unit))
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "CYPHER_TTS: TTS_ERROR (utteranceId=$utteranceId)")
                    _isSpeaking.value = false
                    resumePending(utteranceId, Result.failure(RuntimeException("TTS error on utterance $utteranceId")))
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "CYPHER_TTS: TTS_ERROR (code=$errorCode, utteranceId=$utteranceId)")
                    _isSpeaking.value = false
                    resumePending(utteranceId, Result.failure(RuntimeException("TTS error code=$errorCode")))
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    Log.i(TAG, "CYPHER_TTS: TTS_DONE (stopped, utteranceId=$utteranceId, interrupted=$interrupted)")
                    _isSpeaking.value = false
                    resumePending(utteranceId, Result.success(Unit))
                }
            })

            refreshVoicesAndLanguages()
            _isReady.value = true
        } else {
            Log.e(TAG, "CYPHER_TTS: TTS_INIT_FAILED (status=$status)")
            _isReady.value = false
            synchronized(this) {
                tts = null
            }
        }
    }

    private fun resumePending(utteranceId: String?, result: Result<Unit>) {
        if (utteranceId != null) {
            pendingUtterances.remove(utteranceId)?.resume(result)
        } else {
            Log.w(TAG, "CYPHER_TTS: Null utteranceId in callback -> draining ${pendingUtterances.size} pending continuations")
            val snapshot = pendingUtterances.values.toList()
            pendingUtterances.clear()
            snapshot.forEach { it.resume(result) }
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
            Log.w(TAG, "CYPHER_TTS: Could not enumerate voices", e)
        }
    }

    private fun detectVoiceGender(voice: Voice): Boolean? {
        val nameLower = voice.name.lowercase(Locale.ROOT)
        val features = voice.features?.map { it.lowercase(Locale.ROOT) } ?: emptyList()
        return when {
            nameLower.contains("female") || nameLower.contains("woman") || features.any { it.contains("female") } -> true
            nameLower.contains("male") || nameLower.contains("man") || features.any { it.contains("male") } -> false
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
        if (text.isBlank()) return Result.success(Unit)

        if (tts == null || !_isReady.value) {
            Log.i(TAG, "CYPHER_TTS: speak() called but engine not ready -> ensuring initialization")
            initialize()
            val ready = withTimeoutOrNull(8000L) {
                _isReady.first { it }
            }
            if (ready != true) {
                Log.e(TAG, "CYPHER_TTS: TTS_ERROR - Engine failed to become ready within 8s timeout")
                return Result.failure(IllegalStateException("TTS engine not ready"))
            }
        }

        val engine = tts ?: run {
            Log.e(TAG, "CYPHER_TTS: TTS_ERROR - tts instance is null after readiness check")
            return Result.failure(IllegalStateException("TTS engine is null"))
        }

        Log.d(TAG, "CYPHER_TTS: Speaking utterance [$utteranceId]: \"${text.take(80)}\"")

        return suspendCancellableCoroutine { continuation ->
            pendingUtterances[utteranceId] = continuation

            continuation.invokeOnCancellation {
                pendingUtterances.remove(utteranceId)
            }

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            val result = engine.speak(text, queueMode, params, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                Log.e(TAG, "CYPHER_TTS: TTS_ERROR - engine.speak() returned $result")
                pendingUtterances.remove(utteranceId)
                _isSpeaking.value = false
                scheduleReinit()
                continuation.resume(Result.failure(RuntimeException("speak() failed with code $result")))
            }
        }
    }

    private fun scheduleReinit() {
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
        _isReady.value = false
        Log.i(TAG, "CYPHER_TTS: Reinitializing TTS after non-success return code")
        initialize()
    }

    override fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
            val snapshot = pendingUtterances.values.toList()
            pendingUtterances.clear()
            snapshot.forEach { it.resume(Result.success(Unit)) }
        } catch (e: Exception) {
            Log.e(TAG, "CYPHER_TTS: Error stopping TTS", e)
        }
    }

    override fun setLanguage(locale: Locale): Boolean {
        configuredLocale = locale
        val engine = tts ?: return false
        return try {
            val result = engine.setLanguage(locale)
            result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        } catch (e: Exception) {
            Log.e(TAG, "CYPHER_TTS: Error setting language to $locale", e)
            false
        }
    }

    override fun setVoice(voiceName: String): Boolean {
        configuredVoiceName = voiceName
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
            Log.e(TAG, "CYPHER_TTS: Error setting voice to $voiceName", e)
            false
        }
    }

    override fun setSpeechRate(rate: Float) {
        configuredSpeechRate = rate.coerceIn(0.5f, 2.5f)
        tts?.setSpeechRate(configuredSpeechRate)
    }

    override fun setPitch(pitch: Float) {
        configuredPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(configuredPitch)
    }

    override fun shutdown() {
        Log.i(TAG, "CYPHER_TTS: TTS_SHUTDOWN")
        try {
            val snapshot = pendingUtterances.values.toList()
            pendingUtterances.clear()
            snapshot.forEach { it.resume(Result.success(Unit)) }

            tts?.stop()
            tts?.shutdown()
            tts = null
            _isReady.value = false
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(TAG, "CYPHER_TTS: Error during TTS shutdown", e)
        }
    }
}
