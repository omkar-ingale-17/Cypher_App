package com.cypher.assistant.features.youtube.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.cypher.assistant.features.youtube.domain.model.YouTubeScreenType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight detector that analyzes the active YouTube screen hierarchy to identify the current screen type.
 */
@Singleton
class YouTubeScreenDetector @Inject constructor(
    private val matcher: YouTubeUiMatcher
) {
    fun detectScreen(root: AccessibilityNodeInfo?): YouTubeScreenType {
        if (root == null) return YouTubeScreenType.UNKNOWN

        // 1. Check for search input
        if (matcher.findSearchInput(root) != null) {
            return YouTubeScreenType.SEARCH_INPUT
        }

        // 2. Check for comments overlay
        if (matcher.findCloseCommentsButton(root) != null) {
            return YouTubeScreenType.COMMENTS
        }

        // 3. Check for video player (Like button / Dislike button present)
        val (likeNode, _) = matcher.findLikeButton(root)
        if (likeNode != null || matcher.findDislikeButton(root) != null) {
            return YouTubeScreenType.VIDEO
        }

        // 4. Check for search results vs home
        val candidates = matcher.findVideoCandidates(root)
        if (candidates.size >= 2) {
            return YouTubeScreenType.SEARCH_RESULTS
        }

        // 5. Check for home navigation
        if (matcher.findTabOrNavigationButton(root, "home") != null) {
            return YouTubeScreenType.HOME
        }

        return YouTubeScreenType.UNKNOWN
    }
}
