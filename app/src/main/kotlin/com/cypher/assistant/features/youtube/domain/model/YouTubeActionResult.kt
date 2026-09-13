package com.cypher.assistant.features.youtube.domain.model

/**
 * Result representation for YouTube actions.
 */
sealed interface YouTubeActionResult {
    data class Success(val message: String) : YouTubeActionResult
    data class AlreadyInState(val message: String) : YouTubeActionResult
    data class NotFound(val target: String, val reason: String) : YouTubeActionResult
    data class NotAvailable(val reason: String) : YouTubeActionResult
    data class PermissionRequired(val reason: String) : YouTubeActionResult
    data class ConfirmationRequired(val message: String) : YouTubeActionResult
    data class Failed(val reason: String) : YouTubeActionResult
    data object Timeout : YouTubeActionResult
}
