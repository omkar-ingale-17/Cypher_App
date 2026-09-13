package com.cypher.assistant.features.youtube.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "YouTubeIntentDataSource"
private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

/**
 * Responsible for building and dispatching all YouTube-related Android intents.
 *
 * Strategy priority for each operation:
 * 1. YouTube app deep-link / launch intent (if YouTube is installed)
 * 2. HTTPS URL fallback (opens in default browser)
 *
 * Media controls use AudioManager.dispatchMediaKeyEvent — the officially supported
 * method to send media key events to whichever app holds the active MediaSession.
 */
@Singleton
class YouTubeIntentDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /** Returns true if the YouTube app is installed and launchable. */
    fun isYouTubeInstalled(): Boolean =
        context.packageManager.getLaunchIntentForPackage(YOUTUBE_PACKAGE) != null

    /**
     * Launches the YouTube app home, or falls back to youtube.com.
     * @return true if the intent was dispatched without exception.
     */
    fun launchYouTubeApp(): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(YOUTUBE_PACKAGE)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            dispatchIntent(launchIntent)
        } else {
            openUrl("https://www.youtube.com")
        }
    }

    /**
     * Opens a YouTube search results page for [query].
     * Tries the YouTube app deep-link first, then falls back to browser.
     */
    fun openSearchUrl(query: String): Boolean {
        val encodedQuery = Uri.encode(query)
        // Try YouTube app via vnd.youtube URI (supported on modern YouTube builds)
        val ytUri = Uri.parse("vnd.youtube://results?search_query=$encodedQuery")
        val ytIntent = Intent(Intent.ACTION_VIEW, ytUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = context.packageManager.resolveActivity(ytIntent, 0)
        return if (resolved != null) {
            dispatchIntent(ytIntent)
        } else {
            // Fallback: open in browser
            openUrl("https://www.youtube.com/results?search_query=$encodedQuery")
        }
    }

    /**
     * Opens a well-known YouTube destination by path segment.
     *
     * Supported paths: "shorts", "feed/subscriptions", "feed/history"
     * Falls back to browser if the YouTube app does not handle the URI.
     */
    fun openYouTubeDeepLink(path: String): Boolean {
        // Try vnd.youtube scheme first
        val ytUri = Uri.parse("vnd.youtube://$path")
        val ytIntent = Intent(Intent.ACTION_VIEW, ytUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = context.packageManager.resolveActivity(ytIntent, 0)
        return if (resolved != null) {
            dispatchIntent(ytIntent)
        } else {
            openUrl("https://www.youtube.com/$path")
        }
    }

    /**
     * Opens an arbitrary HTTPS URL via the default browser or any handler.
     */
    fun openUrl(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return dispatchIntent(intent)
    }

    // -------------------------------------------------------------------------
    // Media key events — dispatched via AudioManager (official public API)
    // -------------------------------------------------------------------------

    /** Sends KEY_DOWN + KEY_UP pair for [keyCode] to the active media session. */
    fun sendMediaKey(keyCode: Int) {
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        Log.d(TAG, "Dispatched media key: $keyCode")
    }

    // -------------------------------------------------------------------------
    // Volume — AudioManager STREAM_MUSIC
    // -------------------------------------------------------------------------

    fun volumeUp() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun volumeDown() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun mute() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun unmute() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_UNMUTE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun dispatchIntent(intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity found for intent: ${intent.data}", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException dispatching intent: ${intent.data}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error dispatching intent: ${intent.data}", e)
            false
        }
    }
}
