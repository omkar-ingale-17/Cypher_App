package com.cypher.assistant.features.youtube.accessibility

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves whether YouTube is installed and identifies the specific package name.
 */
@Singleton
class YouTubePackageResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val STANDARD_PACKAGE = "com.google.android.youtube"
        val KNOWN_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.google.android.youtube.tv",
            "com.google.android.youtube.googletv",
            "com.google.android.apps.youtube.music"
        )
    }

    fun getInstalledPackage(): String? {
        val pm = context.packageManager
        for (pkg in KNOWN_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return null
    }

    fun isInstalled(): Boolean = getInstalledPackage() != null
}
