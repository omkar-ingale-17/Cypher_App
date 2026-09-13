package com.cypher.assistant.features.youtube.presentation

import com.cypher.assistant.features.youtube.domain.model.YouTubeScreenType

/**
 * State representation for the YouTube Control UI.
 */
data class YouTubeUiState(
    val isYouTubeInstalled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val currentScreen: YouTubeScreenType = YouTubeScreenType.UNKNOWN,
    val installedPackage: String? = null,
    val lastActionMessage: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)
