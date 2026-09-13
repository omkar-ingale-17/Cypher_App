package com.cypher.assistant.features.applications.domain.model

import android.graphics.drawable.Drawable

/**
 * Representation of an installed, launchable application on the Android device.
 *
 * @param packageName The unique Android package identifier (e.g. "com.whatsapp").
 * @param appName The human-readable application label (e.g. "WhatsApp").
 * @param launchActivityName The main launcher activity component name if available.
 * @param isSystemApp True if pre-installed in the system partition.
 * @param versionName The human-readable version string if available.
 * @param versionCode The internal version code.
 * @param icon The application icon drawable (cached in-memory for UI rendering).
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val launchActivityName: String? = null,
    val isSystemApp: Boolean = false,
    val versionName: String? = null,
    val versionCode: Long = 0L,
    val icon: Drawable? = null
)
