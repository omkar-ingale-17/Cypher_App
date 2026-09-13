package com.cypher.assistant.features.youtube.presentation

/**
 * UI State for YouTube control panel.
 */
sealed interface YouTubeUiState {
    data object Idle : YouTubeUiState
    data object Loading : YouTubeUiState
    data class Success(val message: String) : YouTubeUiState
    data class Error(val errorMessage: String) : YouTubeUiState
}
