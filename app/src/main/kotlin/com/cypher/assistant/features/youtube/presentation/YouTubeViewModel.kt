package com.cypher.assistant.features.youtube.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.youtube.domain.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<YouTubeUiState>(YouTubeUiState.Idle)
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun executeSearch() {
        val q = _searchQuery.value.trim()
        if (q.isNotBlank()) {
            executeAction { youTubeRepository.search(q) }
        }
    }

    fun openYouTube() = executeAction { youTubeRepository.openYouTube() }
    fun openHome() = executeAction { youTubeRepository.openHome() }
    fun openShorts() = executeAction { youTubeRepository.openShorts() }
    fun openSubscriptions() = executeAction { youTubeRepository.openSubscriptions() }
    fun openHistory() = executeAction { youTubeRepository.openHistory() }
    fun openChannel() = executeAction { youTubeRepository.openChannel() }

    fun pause() = executeAction { youTubeRepository.pause() }
    fun resume() = executeAction { youTubeRepository.resume() }
    fun stop() = executeAction { youTubeRepository.stop() }
    fun next() = executeAction { youTubeRepository.next() }
    fun previous() = executeAction { youTubeRepository.previous() }

    fun volumeUp() = executeAction { youTubeRepository.volumeUp() }
    fun volumeDown() = executeAction { youTubeRepository.volumeDown() }
    fun mute() = executeAction { youTubeRepository.mute() }
    fun unmute() = executeAction { youTubeRepository.unmute() }

    private fun executeAction(action: suspend () -> CommandResult) {
        viewModelScope.launch {
            _uiState.value = YouTubeUiState.Loading
            val result = action()
            _uiState.value = if (result.success) {
                YouTubeUiState.Success(result.message)
            } else {
                YouTubeUiState.Error(result.message)
            }
        }
    }

    fun resetState() {
        _uiState.value = YouTubeUiState.Idle
    }
}
