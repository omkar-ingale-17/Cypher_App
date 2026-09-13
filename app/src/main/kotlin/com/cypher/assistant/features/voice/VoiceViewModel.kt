package com.cypher.assistant.features.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cypher.assistant.core.voice.VoiceEngine
import com.cypher.assistant.core.voice.VoiceState
import com.cypher.assistant.core.voice.tts.VoiceInfo
import com.cypher.assistant.data.database.entities.CommandHistoryEntity
import com.cypher.assistant.data.preferences.UserPreferences
import com.cypher.assistant.data.preferences.UserPreferencesDataStore
import com.cypher.assistant.data.repository.CommandHistoryRepository
import com.cypher.assistant.services.voice.VoiceAssistantService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * UI State for [VoiceScreen] in a pure hands-free, wake-word-activated paradigm.
 */
data class VoiceUiState(
    val voiceState: VoiceState = VoiceState.IDLE,
    val liveTranscript: String = "",
    val lastResponse: String = "",
    val statusMessage: String = "Say 'Cypher', 'Jan', 'Jaan', or 'Baby'",
    val rmsLevel: Float = 0f,
    val errorMessage: String? = null,
    val userPreferences: UserPreferences = UserPreferences(),
    val availableVoices: List<VoiceInfo> = emptyList(),
    val availableLanguages: List<Locale> = emptyList(),
    val isSettingsSheetOpen: Boolean = false,
    val isOnboardingOpen: Boolean = false,
    val isApplicationsSheetOpen: Boolean = false
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceEngine: VoiceEngine,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val historyRepository: CommandHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    val commandHistory: StateFlow<List<CommandHistoryEntity>> =
        historyRepository.observeRecentHistory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    init {
        observeVoiceEngine()
        observePreferences()
    }

    private fun observeVoiceEngine() {
        viewModelScope.launch {
            combine(
                voiceEngine.voiceState,
                voiceEngine.currentTranscript,
                voiceEngine.lastResponse,
                voiceEngine.rmsLevel
            ) { state, transcript, response, rms ->
                val statusText = when (state) {
                    VoiceState.IDLE -> "Say 'Cypher', 'Jan', 'Jaan', or 'Baby' to activate"
                    VoiceState.LISTENING_FOR_WAKE_WORD -> "Listening for wake word..."
                    VoiceState.WAKE_WORD_DETECTED -> "Wake word detected"
                    VoiceState.LISTENING_FOR_COMMAND -> "Listening for command..."
                    VoiceState.PROCESSING -> "Processing command..."
                    VoiceState.SPEAKING -> "Speaking response..."
                    VoiceState.ERROR -> "Say 'Cypher' to activate"
                }

                _uiState.update { current ->
                    current.copy(
                        voiceState = state,
                        liveTranscript = transcript,
                        lastResponse = response,
                        statusMessage = statusText,
                        rmsLevel = rms
                    )
                }
            }.collect {}
        }

        viewModelScope.launch {
            voiceEngine.availableVoices.collect { voices ->
                _uiState.update { it.copy(availableVoices = voices) }
            }
        }

        viewModelScope.launch {
            voiceEngine.availableLanguages.collect { languages ->
                _uiState.update { it.copy(availableLanguages = languages) }
            }
        }

        viewModelScope.launch {
            voiceEngine.errorMessages.collect { errorMsg ->
                _uiState.update { it.copy(errorMessage = errorMsg, voiceState = VoiceState.ERROR) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesDataStore.userPreferences.collect { prefs ->
                _uiState.update {
                    it.copy(
                        userPreferences = prefs,
                        isOnboardingOpen = !prefs.onboardingComplete && prefs.userName == "Commander"
                    )
                }

                // Apply preferences to voice engine
                if (prefs.selectedLanguageTag != "default") {
                    val locale = Locale.forLanguageTag(prefs.selectedLanguageTag)
                    voiceEngine.setLanguage(locale)
                }
                if (prefs.selectedVoiceName.isNotBlank()) {
                    voiceEngine.setVoice(prefs.selectedVoiceName)
                }
                voiceEngine.setSpeechRate(prefs.ttsSpeechRate)
                voiceEngine.setPitch(prefs.ttsPitch)
            }
        }
    }

    fun startStandbyListening(context: Context? = null) {
        val prefs = _uiState.value.userPreferences
        if (prefs.wakeWordEnabled) {
            if (context != null) {
                VoiceAssistantService.start(context)
            } else if (_uiState.value.voiceState == VoiceState.IDLE) {
                voiceEngine.startListening(
                    requireWakePhrase = true
                )
            }
        }
    }

    fun onTextCommandSubmitted(text: String) {
        if (text.isBlank()) return
        voiceEngine.processTextInput(text)
    }


    fun openApplicationsSheet() {
        _uiState.update { it.copy(isApplicationsSheetOpen = true) }
    }

    fun closeApplicationsSheet() {
        _uiState.update { it.copy(isApplicationsSheetOpen = false) }
    }

    fun openSettingsSheet() {
        _uiState.update { it.copy(isSettingsSheetOpen = true) }
    }

    fun closeSettingsSheet() {
        _uiState.update { it.copy(isSettingsSheetOpen = false) }
    }

    fun onSetUserName(name: String) {
        viewModelScope.launch {
            preferencesDataStore.setUserName(name)
            preferencesDataStore.setOnboardingComplete(true)
            _uiState.update { it.copy(isOnboardingOpen = false) }
        }
    }

    fun onDismissOnboarding() {
        viewModelScope.launch {
            preferencesDataStore.setOnboardingComplete(true)
            _uiState.update { it.copy(isOnboardingOpen = false) }
        }
    }

    fun onLanguageSelected(locale: Locale) {
        viewModelScope.launch {
            preferencesDataStore.setSelectedLanguageTag(locale.toLanguageTag())
            voiceEngine.setLanguage(locale)
        }
    }

    fun onVoiceSelected(voiceInfo: VoiceInfo) {
        viewModelScope.launch {
            preferencesDataStore.setSelectedVoiceName(voiceInfo.name)
            voiceEngine.setVoice(voiceInfo.name)
        }
    }

    fun onSpeechRateChanged(rate: Float) {
        viewModelScope.launch {
            preferencesDataStore.setTtsSpeechRate(rate)
            voiceEngine.setSpeechRate(rate)
        }
    }

    fun onPitchChanged(pitch: Float) {
        viewModelScope.launch {
            preferencesDataStore.setTtsPitch(pitch)
            voiceEngine.setPitch(pitch)
        }
    }

    fun onToggleContinuousListening(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setContinuousListening(enabled)
        }
    }

    fun onToggleWakeWord(enabled: Boolean, context: Context? = null) {
        viewModelScope.launch {
            preferencesDataStore.setWakeWordEnabled(enabled)
            if (enabled) {
                if (context != null) {
                    VoiceAssistantService.start(context)
                } else {
                    startStandbyListening()
                }
            } else {
                if (context != null) {
                    VoiceAssistantService.stop(context)
                }
                if (_uiState.value.voiceState == VoiceState.LISTENING_FOR_WAKE_WORD) {
                    voiceEngine.stopListening()
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
        if (_uiState.value.voiceState == VoiceState.ERROR) {
            voiceEngine.cancel()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Do not cancel voiceEngine if background service is enabled
        viewModelScope.launch {
            val prefs = preferencesDataStore.userPreferences.first()
            if (!prefs.wakeWordEnabled && !prefs.continuousListening) {
                voiceEngine.cancel()
            }
        }
    }
}
