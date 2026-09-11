package com.cypher.assistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cypher.assistant.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.DATASTORE_NAME
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TTS_ENABLED             = booleanPreferencesKey("tts_enabled")
        val TTS_SPEECH_RATE         = floatPreferencesKey("tts_speech_rate")
        val TTS_PITCH               = floatPreferencesKey("tts_pitch")
        val SELECTED_VOICE_NAME     = stringPreferencesKey("selected_voice_name")
        val SELECTED_LANGUAGE_TAG   = stringPreferencesKey("selected_language_tag")
        val WAKE_WORD_ENABLED       = booleanPreferencesKey("wake_word_enabled")
        val CONTINUOUS_LISTENING    = booleanPreferencesKey("continuous_listening")
        val CONFIRMATION_REQUIRED   = booleanPreferencesKey("confirmation_required")
        val HISTORY_ENABLED         = booleanPreferencesKey("history_enabled")
        val DARK_MODE               = booleanPreferencesKey("dark_mode")
        val ONBOARDING_COMPLETE     = booleanPreferencesKey("onboarding_complete")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            UserPreferences(
                ttsEnabled          = prefs[Keys.TTS_ENABLED]           ?: true,
                ttsSpeechRate       = prefs[Keys.TTS_SPEECH_RATE]       ?: 1.0f,
                ttsPitch            = prefs[Keys.TTS_PITCH]             ?: 1.0f,
                selectedVoiceName   = prefs[Keys.SELECTED_VOICE_NAME]   ?: "",
                selectedLanguageTag = prefs[Keys.SELECTED_LANGUAGE_TAG] ?: "default",
                wakeWordEnabled     = prefs[Keys.WAKE_WORD_ENABLED]     ?: true,
                continuousListening = prefs[Keys.CONTINUOUS_LISTENING]  ?: false,
                confirmationRequired= prefs[Keys.CONFIRMATION_REQUIRED] ?: true,
                historyEnabled      = prefs[Keys.HISTORY_ENABLED]       ?: true,
                darkMode            = prefs[Keys.DARK_MODE]             ?: true,
                onboardingComplete  = prefs[Keys.ONBOARDING_COMPLETE]   ?: false
            )
        }

    suspend fun setTtsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.TTS_ENABLED] = enabled }

    suspend fun setTtsSpeechRate(rate: Float) =
        context.dataStore.edit { it[Keys.TTS_SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f) }

    suspend fun setTtsPitch(pitch: Float) =
        context.dataStore.edit { it[Keys.TTS_PITCH] = pitch.coerceIn(0.5f, 2.0f) }

    suspend fun setSelectedVoiceName(voiceName: String) =
        context.dataStore.edit { it[Keys.SELECTED_VOICE_NAME] = voiceName }

    suspend fun setSelectedLanguageTag(tag: String) =
        context.dataStore.edit { it[Keys.SELECTED_LANGUAGE_TAG] = tag }

    suspend fun setWakeWordEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }

    suspend fun setContinuousListening(enabled: Boolean) =
        context.dataStore.edit { it[Keys.CONTINUOUS_LISTENING] = enabled }

    suspend fun setConfirmationRequired(required: Boolean) =
        context.dataStore.edit { it[Keys.CONFIRMATION_REQUIRED] = required }

    suspend fun setHistoryEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.HISTORY_ENABLED] = enabled }

    suspend fun setDarkMode(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }

    suspend fun setOnboardingComplete(complete: Boolean) =
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
}

data class UserPreferences(
    val ttsEnabled: Boolean           = true,
    val ttsSpeechRate: Float          = 1.0f,
    val ttsPitch: Float               = 1.0f,
    val selectedVoiceName: String     = "",
    val selectedLanguageTag: String   = "default",
    val wakeWordEnabled: Boolean      = true,
    val continuousListening: Boolean  = false,
    val confirmationRequired: Boolean = true,
    val historyEnabled: Boolean       = true,
    val darkMode: Boolean             = true,
    val onboardingComplete: Boolean   = false
)
