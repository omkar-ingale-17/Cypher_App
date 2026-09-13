package com.cypher.assistant.features.youtube.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches official intents and deep links directly targeted to the installed YouTube package.
 */
@Singleton
class YouTubeIntentDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appResolver: YouTubeAppResolver
) {
    fun launchSearch(query: String): Boolean {
        val pkg = appResolver.getYouTubePackage() ?: return false
        return try {
            val uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun launchShorts(): Boolean {
        return launchDeepLink("https://www.youtube.com/shorts")
    }

    fun launchSubscriptions(): Boolean {
        return launchDeepLink("https://www.youtube.com/feed/subscriptions")
    }

    fun launchHistory(): Boolean {
        return launchDeepLink("https://www.youtube.com/feed/history")
    }

    fun launchNotifications(): Boolean {
        return launchDeepLink("https://www.youtube.com/feed/notifications")
    }

    fun launchHome(): Boolean {
        val pkg = appResolver.getYouTubePackage() ?: return false
        val launchIntent = appResolver.getLaunchIntent() ?: return false
        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun launchDeepLink(url: String): Boolean {
        val pkg = appResolver.getYouTubePackage() ?: return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback to opening home
            launchHome()
        }
    }
}
