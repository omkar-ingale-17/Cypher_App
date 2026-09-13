package com.cypher.assistant.features.youtube.data

import android.content.Context
import android.content.Intent
import com.cypher.assistant.features.youtube.accessibility.YouTubePackageResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolver for checking YouTube application presence and retrieving launch intents.
 */
@Singleton
class YouTubeAppResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageResolver: YouTubePackageResolver
) {
    fun isYouTubeInstalled(): Boolean = packageResolver.isInstalled()

    fun getYouTubePackage(): String? = packageResolver.getInstalledPackage()

    fun getLaunchIntent(): Intent? {
        val pkg = getYouTubePackage() ?: return null
        return context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
