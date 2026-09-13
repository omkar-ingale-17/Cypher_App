package com.cypher.assistant.features.applications.data.launcher

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.applications.domain.model.AppInfo
import com.cypher.assistant.features.applications.domain.model.LaunchResult
import com.cypher.assistant.services.accessibility.CypherAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level Android application launch and system navigation execution handler.
 * Uses official Android Intent, PackageManager, and AccessibilityService APIs.
 */
@Singleton
class AndroidApplicationLauncher @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "CYPHER_APP_LAUNCH"
    }

    /**
     * Launches the given [appInfo] using its launcher intent.
     * Supports reliable launching when Cypher is in background/minimized.
     */
    fun launch(appInfo: AppInfo): LaunchResult {
        val packageManager = context.packageManager

        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent == null) {
                Log.w(TAG, "No launch intent found for package: ${appInfo.packageName}")
                return LaunchResult.Disabled(
                    appInfo = appInfo,
                    message = "I found ${appInfo.appName}, but it cannot be launched directly."
                )
            }

            // Flags to bring target app to front and create a new task
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            )

            var launchedSuccessfully = false

            // Strategy 1: If Accessibility Service is running, use it to start activity (bypasses BAL restrictions)
            val accessibilityService = CypherAccessibilityService.instance
            if (accessibilityService != null) {
                try {
                    accessibilityService.startActivity(launchIntent)
                    launchedSuccessfully = true
                    Log.i(TAG, "Launched ${appInfo.appName} via AccessibilityService")
                } catch (e: Exception) {
                    Log.w(TAG, "AccessibilityService.startActivity failed, falling back to PendingIntent", e)
                }
            }

            // Strategy 2: PendingIntent with background activity start allowed (Android 14+ / API 34+)
            if (!launchedSuccessfully) {
                try {
                    val requestCode = (System.currentTimeMillis() % 10000).toInt()
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        requestCode,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val options = ActivityOptions.makeBasic().apply {
                            pendingIntentBackgroundActivityStartMode =
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        }.toBundle()
                        pendingIntent.send(context, 0, null, null, null, null, options)
                    } else {
                        pendingIntent.send()
                    }
                    launchedSuccessfully = true
                    Log.i(TAG, "Launched ${appInfo.appName} via PendingIntent")
                } catch (e: Exception) {
                    Log.w(TAG, "PendingIntent launch failed, falling back to context.startActivity", e)
                }
            }

            // Strategy 3: Standard context.startActivity fallback
            if (!launchedSuccessfully) {
                context.startActivity(launchIntent)
                Log.i(TAG, "Launched ${appInfo.appName} via context.startActivity")
            }

            return LaunchResult.Success(
                appInfo = appInfo,
                message = "Opening ${appInfo.appName}."
            )

        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "ActivityNotFoundException launching ${appInfo.packageName}", e)
            return LaunchResult.Failed(
                appName = appInfo.appName,
                error = e.message.orEmpty(),
                message = "I couldn't open ${appInfo.appName}."
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException launching ${appInfo.packageName}", e)
            return LaunchResult.Failed(
                appName = appInfo.appName,
                error = e.message.orEmpty(),
                message = "Permission denied while trying to open ${appInfo.appName}."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error launching ${appInfo.packageName}", e)
            return LaunchResult.Failed(
                appName = appInfo.appName,
                error = e.message.orEmpty(),
                message = "I couldn't open ${appInfo.appName}."
            )
        }
    }

    /**
     * Navigates to the Home screen using standard Android Intent or AccessibilityService.
     */
    fun launchHome(): LaunchResult {
        return try {
            if (CypherAccessibilityService.isServiceRunning()) {
                CypherAccessibilityService.goHome()
            }
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(homeIntent)
            Log.i(TAG, "Successfully navigated to Home screen")
            LaunchResult.Success(
                appInfo = AppInfo(packageName = "android.intent.category.HOME", appName = "Home"),
                message = "Going to home screen."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to Home screen", e)
            LaunchResult.Failed(
                appName = "Home",
                error = e.message.orEmpty(),
                message = "I couldn't return to the home screen."
            )
        }
    }

    /**
     * Locks the screen using Android-supported APIs:
     * 1. AccessibilityService GLOBAL_ACTION_LOCK_SCREEN (Android 9+)
     * 2. DevicePolicyManager lockNow() if admin active
     * 3. Graceful fallback telling user what permission is required
     */
    fun lockScreen(): CommandResult {
        // Strategy 1: Accessibility Service (Android 9+)
        if (CypherAccessibilityService.isServiceRunning()) {
            val locked = CypherAccessibilityService.lockScreen()
            if (locked) {
                Log.i(TAG, "Screen locked via CypherAccessibilityService")
                return CommandResult.success("Locking the screen.")
            }
        }

        // Strategy 2: DevicePolicyManager (if device administrator is granted)
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            if (dpm != null) {
                val adminComponent = ComponentName(context, context.packageName)
                if (dpm.isAdminActive(adminComponent)) {
                    dpm.lockNow()
                    Log.i(TAG, "Screen locked via DevicePolicyManager")
                    return CommandResult.success("Locking the screen.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DevicePolicyManager lock failed", e)
        }

        // Strategy 3: Gracefully inform user about Accessibility requirement
        Log.w(TAG, "Cannot lock screen: Accessibility Service not enabled")
        return CommandResult.failure(
            "To lock the screen, please enable Cypher in your phone's Accessibility Settings."
        )
    }

    /**
     * Handles app closing requests safely within Android security sandbox.
     */
    fun closeApp(appName: String): CommandResult {
        val target = appName.trim()

        // Safely navigate to home screen
        launchHome()

        return if (target.isBlank() || target.equals("cypher", ignoreCase = true) || target.equals("app", ignoreCase = true)) {
            CommandResult.success("Going to home screen.")
        } else {
            CommandResult.success("Returned to home. Android's security sandbox does not allow normal assistant apps to force-stop background applications.")
        }
    }

    /**
     * Opens common Android system settings screens.
     */
    fun launchSystemScreen(screenType: String): LaunchResult {
        val intentAction = when (screenType.lowercase()) {
            "settings", "system settings" -> Settings.ACTION_SETTINGS
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps", "application settings" -> Settings.ACTION_APPLICATION_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "Successfully opened system screen: $screenType ($intentAction)")
            LaunchResult.Success(
                appInfo = AppInfo(packageName = "com.android.settings", appName = "Settings"),
                message = "Opening settings."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening system screen: $screenType", e)
            LaunchResult.Failed(
                appName = "Settings",
                error = e.message.orEmpty(),
                message = "I couldn't open settings."
            )
        }
    }
}
