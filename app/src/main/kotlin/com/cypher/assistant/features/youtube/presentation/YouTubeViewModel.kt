package com.cypher.assistant.features.youtube.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cypher.assistant.features.youtube.domain.model.YouTubeActionResult
import com.cypher.assistant.features.youtube.domain.model.YouTubeCommand
import com.cypher.assistant.features.youtube.domain.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeViewModel @Inject constructor(
    private val repository: YouTubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        YouTubeUiState(
            isYouTubeInstalled = repository.isYouTubeInstalled,
            isAccessibilityEnabled = repository.isAccessibilityEnabled,
            installedPackage = repository.getInstalledPackage()
        )
    )
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    init {
        observeScreen()
    }

    private fun observeScreen() {
        viewModelScope.launch {
            repository.currentScreen.collect { screen ->
                _uiState.update { it.copy(currentScreen = screen) }
            }
        }
    }

    fun refreshStatus() {
        _uiState.update {
            it.copy(
                isYouTubeInstalled = repository.isYouTubeInstalled,
                isAccessibilityEnabled = repository.isAccessibilityEnabled,
                installedPackage = repository.getInstalledPackage()
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun executeCommand(command: YouTubeCommand) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.execute(command)
            val message = when (result) {
                is YouTubeActionResult.Success -> result.message
                is YouTubeActionResult.AlreadyInState -> result.message
                is YouTubeActionResult.NotFound -> result.reason
                is YouTubeActionResult.NotAvailable -> result.reason
                is YouTubeActionResult.PermissionRequired -> result.reason
                is YouTubeActionResult.ConfirmationRequired -> result.message
                is YouTubeActionResult.Failed -> result.reason
                is YouTubeActionResult.Timeout -> "Action timed out"
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    lastActionMessage = message,
                    isAccessibilityEnabled = repository.isAccessibilityEnabled
                )
            }
        }
    }

    fun openAccessibilitySettings() {
        repository.openAccessibilitySettings()
    }
}
