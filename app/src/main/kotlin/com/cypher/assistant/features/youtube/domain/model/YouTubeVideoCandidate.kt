package com.cypher.assistant.features.youtube.domain.model

import android.view.accessibility.AccessibilityNodeInfo

/**
 * A candidate video container identified on the YouTube search results screen.
 */
data class YouTubeVideoCandidate(
    val title: String,
    val node: AccessibilityNodeInfo?,
    val confidence: Float
)
