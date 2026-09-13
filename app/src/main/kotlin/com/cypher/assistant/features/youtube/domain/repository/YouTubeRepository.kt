package com.cypher.assistant.features.youtube.domain.repository

import com.cypher.assistant.features.youtube.domain.model.YouTubeActionResult
import com.cypher.assistant.features.youtube.domain.model.YouTubeCommand
import com.cypher.assistant.features.youtube.domain.model.YouTubeScreenType
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface defining all supported YouTube control actions.
 */
interface YouTubeRepository {
    val currentScreen: StateFlow<YouTubeScreenType>
    val isYouTubeInstalled: Boolean
    val isAccessibilityEnabled: Boolean

    suspend fun execute(command: YouTubeCommand): YouTubeActionResult
    fun getInstalledPackage(): String?
    fun openAccessibilitySettings()
}
