package com.cypher.assistant.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Cypher Accessibility Service.
 *
 * Provides official system-level accessibility actions supported by Android:
 * - GLOBAL_ACTION_LOCK_SCREEN (Android 9+ / API 28+)
 * - GLOBAL_ACTION_HOME
 * - GLOBAL_ACTION_BACK
 * - GLOBAL_ACTION_RECENTS
 *
 * Also provides safe, non-intrusive UI automation for legitimate voice-assistant actions
 * on supported apps (such as YouTube).
 */
class CypherAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CypherAccessibility"

        @Volatile
        var instance: CypherAccessibilityService? = null
            private set

        @Volatile
        var currentForegroundPackage: String? = null
            private set

        fun isServiceRunning(): Boolean = instance != null

        fun lockScreen(): Boolean {
            val service = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Log.i(TAG, "Performing GLOBAL_ACTION_LOCK_SCREEN via AccessibilityService")
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
        }

        fun goHome(): Boolean {
            val service = instance ?: return false
            Log.i(TAG, "Performing GLOBAL_ACTION_HOME via AccessibilityService")
            return service.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun goBack(): Boolean {
            val service = instance ?: return false
            Log.i(TAG, "Performing GLOBAL_ACTION_BACK via AccessibilityService")
            return service.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun openRecents(): Boolean {
            val service = instance ?: return false
            Log.i(TAG, "Performing GLOBAL_ACTION_RECENTS via AccessibilityService")
            return service.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        fun launchIntent(intent: Intent): Boolean {
            val service = instance ?: return false
            return try {
                service.startActivity(intent)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch intent via AccessibilityService", e)
                false
            }
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open accessibility settings", e)
            }
        }

        // --- Safe UI Traversal & Interaction Helpers ---

        fun getRootNode(): AccessibilityNodeInfo? {
            val service = instance ?: return null
            return try {
                service.rootInActiveWindow
            } catch (e: Exception) {
                Log.e(TAG, "Error obtaining root node", e)
                null
            }
        }

        fun isYouTubeForeground(knownPackages: Set<String>): Boolean {
            val fg = currentForegroundPackage
            if (fg != null && knownPackages.contains(fg)) return true

            val root = getRootNode() ?: return false
            val rootPkg = root.packageName?.toString()
            return rootPkg != null && knownPackages.contains(rootPkg)
        }

        fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
            val root = getRootNode() ?: return null
            return findNodeRecursive(root, predicate, maxDepth = 25)
        }

        fun findNodes(predicate: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
            val root = getRootNode() ?: return emptyList()
            val results = mutableListOf<AccessibilityNodeInfo>()
            collectNodesRecursive(root, predicate, results, maxDepth = 25)
            return results
        }

        private fun findNodeRecursive(
            node: AccessibilityNodeInfo,
            predicate: (AccessibilityNodeInfo) -> Boolean,
            maxDepth: Int
        ): AccessibilityNodeInfo? {
            if (maxDepth <= 0 || node.isPassword) return null
            if (predicate(node)) return node

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findNodeRecursive(child, predicate, maxDepth - 1)
                if (found != null) return found
            }
            return null
        }

        private fun collectNodesRecursive(
            node: AccessibilityNodeInfo,
            predicate: (AccessibilityNodeInfo) -> Boolean,
            results: MutableList<AccessibilityNodeInfo>,
            maxDepth: Int
        ) {
            if (maxDepth <= 0 || node.isPassword) return
            if (predicate(node)) {
                results.add(node)
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                collectNodesRecursive(child, predicate, results, maxDepth - 1)
            }
        }

        fun clickNode(node: AccessibilityNodeInfo): Boolean {
            if (node.isClickable) {
                val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) return true
            }

            var current: AccessibilityNodeInfo? = node.parent
            var depth = 0
            while (current != null && depth < 3) {
                if (current.isClickable) {
                    val clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
                current = current.parent
                depth++
            }
            return false
        }

        fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        fun scrollForward(node: AccessibilityNodeInfo? = null): Boolean {
            val target = node ?: findFirstScrollableNode() ?: return false
            return target.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        }

        fun scrollBackward(node: AccessibilityNodeInfo? = null): Boolean {
            val target = node ?: findFirstScrollableNode() ?: return false
            return target.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }

        private fun findFirstScrollableNode(): AccessibilityNodeInfo? {
            return findNode { it.isScrollable }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "CypherAccessibilityService connected and active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString()
        if (!pkg.isNullOrBlank()) {
            currentForegroundPackage = pkg
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "CypherAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "CypherAccessibilityService destroyed")
    }
}
