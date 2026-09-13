package com.cypher.assistant.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Cypher Accessibility Service.
 *
 * Provides official system-level accessibility actions supported by Android:
 * - GLOBAL_ACTION_LOCK_SCREEN (Android 9+ / API 28+)
 * - GLOBAL_ACTION_HOME
 * - GLOBAL_ACTION_RECENTS
 */
class CypherAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CypherAccessibility"

        @Volatile
        var instance: CypherAccessibilityService? = null
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

        fun openRecents(): Boolean {
            val service = instance ?: return false
            Log.i(TAG, "Performing GLOBAL_ACTION_RECENTS via AccessibilityService")
            return service.performGlobalAction(GLOBAL_ACTION_RECENTS)
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
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "CypherAccessibilityService connected and active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing for window state changes if needed
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
