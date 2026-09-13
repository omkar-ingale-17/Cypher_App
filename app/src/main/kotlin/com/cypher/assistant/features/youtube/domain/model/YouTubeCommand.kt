package com.cypher.assistant.features.youtube.domain.model

/**
 * Parsed YouTube voice command with extracted query and confidence.
 */
data class YouTubeCommand(
    val query: String,
    val confidence: Float
)
