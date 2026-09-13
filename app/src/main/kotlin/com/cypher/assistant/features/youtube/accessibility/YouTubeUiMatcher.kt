package com.cypher.assistant.features.youtube.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.cypher.assistant.features.youtube.domain.model.YouTubeVideoCandidate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resilient semantic matcher for identifying YouTube UI elements without relying on coordinates.
 */
@Singleton
class YouTubeUiMatcher @Inject constructor() {

    fun findSearchButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNodeMatching(root) { node ->
            val desc = node.contentDescription?.toString()?.trim()
            val text = node.text?.toString()?.trim()
            val resId = node.viewIdResourceName?.lowercase()

            (desc != null && YouTubeSelectors.SEARCH_BUTTON_DESCRIPTIONS.any { desc.equals(it, ignoreCase = true) }) ||
            (text != null && YouTubeSelectors.SEARCH_BUTTON_DESCRIPTIONS.any { text.equals(it, ignoreCase = true) }) ||
            (resId != null && resId.contains("search_button")) ||
            (desc != null && desc.contains("Search", ignoreCase = true) && node.isClickable)
        }
    }

    fun findSearchInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNodeMatching(root) { node ->
            val cls = node.className?.toString()
            val resId = node.viewIdResourceName?.lowercase()
            val text = node.text?.toString()?.trim()

            (cls != null && cls.contains("EditText", ignoreCase = true)) ||
            (resId != null && YouTubeSelectors.SEARCH_INPUT_IDS.any { resId.contains(it) }) ||
            (node.isEditable) ||
            (text != null && text.contains("Search", ignoreCase = true) && node.isFocusable)
        }
    }

    fun findVideoCandidates(root: AccessibilityNodeInfo, targetQuery: String = ""): List<YouTubeVideoCandidate> {
        val candidates = mutableListOf<YouTubeVideoCandidate>()
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        collectClickableNodes(root, clickableNodes, maxDepth = 20)

        for (node in clickableNodes) {
            val desc = node.contentDescription?.toString()?.trim()
            val text = node.text?.toString()?.trim()
            val titleCandidate = when {
                !desc.isNullOrBlank() && desc.length > 5 -> desc
                !text.isNullOrBlank() && text.length > 5 -> text
                else -> null
            }

            if (titleCandidate != null) {
                // Filter out standard navigation/action buttons from video candidates
                val lower = titleCandidate.lowercase()
                if (lower == "home" || lower == "shorts" || lower == "subscriptions" || lower == "you" ||
                    lower == "search" || lower.startsWith("like") || lower.startsWith("subscribe")
                ) {
                    continue
                }

                val confidence = calculateConfidence(titleCandidate, targetQuery)
                candidates.add(YouTubeVideoCandidate(titleCandidate, node, confidence))
            }
        }

        return candidates.sortedByDescending { it.confidence }
    }

    fun findLikeButton(root: AccessibilityNodeInfo): Pair<AccessibilityNodeInfo?, Boolean> {
        var buttonNode: AccessibilityNodeInfo? = null
        var isLiked = false

        findNodeMatching(root) { node ->
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val text = node.text?.toString()?.trim() ?: ""
            val lowerDesc = desc.lowercase()
            val lowerText = text.lowercase()

            if (YouTubeSelectors.LIKED_BUTTON_DESCRIPTIONS.any { lowerDesc.contains(it) || lowerText.contains(it) } ||
                node.isSelected
            ) {
                buttonNode = node
                isLiked = true
                return@findNodeMatching true
            }

            if (YouTubeSelectors.LIKE_BUTTON_DESCRIPTIONS.any { lowerDesc.contains(it) || lowerText.contains(it) }) {
                buttonNode = node
                isLiked = false
                return@findNodeMatching true
            }

            false
        }

        return Pair(buttonNode, isLiked)
    }

    fun findDislikeButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNodeMatching(root) { node ->
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val text = node.text?.toString()?.trim() ?: ""
            val lowerDesc = desc.lowercase()
            val lowerText = text.lowercase()

            YouTubeSelectors.DISLIKE_BUTTON_DESCRIPTIONS.any { lowerDesc.contains(it) || lowerText.contains(it) }
        }
    }

    fun findSubscribeButton(root: AccessibilityNodeInfo): Pair<AccessibilityNodeInfo?, Boolean> {
        var buttonNode: AccessibilityNodeInfo? = null
        var isSubscribed = false

        findNodeMatching(root) { node ->
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val text = node.text?.toString()?.trim() ?: ""
            val combined = ("$desc $text").lowercase()

            if (YouTubeSelectors.SUBSCRIBED_BUTTON_TEXTS.any { combined.contains(it) }) {
                buttonNode = node
                isSubscribed = true
                return@findNodeMatching true
            }

            if (YouTubeSelectors.SUBSCRIBE_BUTTON_TEXTS.any { combined.contains(it) }) {
                buttonNode = node
                isSubscribed = false
                return@findNodeMatching true
            }

            false
        }

        return Pair(buttonNode, isSubscribed)
    }

    fun findCommentsSection(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNodeMatching(root) { node ->
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val text = node.text?.toString()?.trim() ?: ""
            val resId = node.viewIdResourceName?.lowercase() ?: ""

            resId.contains("comment") ||
            YouTubeSelectors.COMMENTS_SECTION_PATTERNS.any {
                desc.contains(it, ignoreCase = true) || text.contains(it, ignoreCase = true)
            }
        }
    }

    fun findCloseCommentsButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNodeMatching(root) { node ->
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val resId = node.viewIdResourceName?.lowercase() ?: ""

            (resId.contains("close") && node.isClickable) ||
            YouTubeSelectors.CLOSE_COMMENTS_DESCRIPTIONS.any { desc.equals(it, ignoreCase = true) }
        }
    }

    fun findShowMoreButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNodeMatching(root) { node ->
            val text = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val combined = ("$text $desc").lowercase()

            YouTubeSelectors.SHOW_MORE_TEXTS.any { combined.contains(it) } && (node.isClickable || node.parent?.isClickable == true)
        }
    }

    fun findShowLessButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNodeMatching(root) { node ->
            val text = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val combined = ("$text $desc").lowercase()

            YouTubeSelectors.SHOW_LESS_TEXTS.any { combined.contains(it) } && (node.isClickable || node.parent?.isClickable == true)
        }
    }

    fun findTabOrNavigationButton(root: AccessibilityNodeInfo, tabKey: String): AccessibilityNodeInfo? {
        val targets = YouTubeSelectors.NAVIGATION_TABS[tabKey.lowercase()] ?: listOf(tabKey)
        return findNodeMatching(root) { node ->
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val text = node.text?.toString()?.trim() ?: ""
            targets.any { desc.equals(it, ignoreCase = true) || text.equals(it, ignoreCase = true) }
        }
    }

    private fun calculateConfidence(title: String, query: String): Float {
        if (query.isBlank()) return 0.75f
        val cleanTitle = title.lowercase()
        val queryTokens = query.lowercase().split(" ").filter { it.isNotBlank() }
        if (queryTokens.isEmpty()) return 0.5f

        val matchedTokens = queryTokens.count { cleanTitle.contains(it) }
        val tokenMatchRatio = matchedTokens.toFloat() / queryTokens.size.toFloat()

        return if (cleanTitle.contains(query.lowercase())) {
            0.95f
        } else {
            0.5f + (tokenMatchRatio * 0.45f)
        }
    }

    private fun findNodeMatching(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeMatching(child, predicate)
            if (found != null) return found
        }
        return null
    }

    private fun collectClickableNodes(
        node: AccessibilityNodeInfo,
        results: MutableList<AccessibilityNodeInfo>,
        maxDepth: Int
    ) {
        if (maxDepth <= 0 || node.isPassword) return
        if (node.isClickable && (!node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank())) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectClickableNodes(child, results, maxDepth - 1)
        }
    }
}
