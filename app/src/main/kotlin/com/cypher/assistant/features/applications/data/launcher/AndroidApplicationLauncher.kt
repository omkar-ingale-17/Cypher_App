package com.cypher.assistant.features.applications.data.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.cypher.assistant.features.applications.domain.model.AppInfo
import com.cypher.assistant.features.applications.domain.model.LaunchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level Android application launch execution handler.
 * Uses official Android Intent and PackageManager APIs.
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

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(launchIntent)
            Log.i(TAG, "Successfully launched application: ${appInfo.appName} (${appInfo.packageName})")

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
