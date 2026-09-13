package com.cypher.assistant.features.youtube.domain.repository

import com.cypher.assistant.core.command.CommandResult

/**
 * Contract for all YouTube control operations available through Cypher.
 *
 * All operations use official/public Android APIs only:
 * - URL intents for open/search/destinations
 * - AudioManager.dispatchMediaKeyEvent for media controls
 * - AudioManager.adjustStreamVolume for volume
 *
 * No shell, ADB, root, AccessibilityService button-pressing, or private APIs are used.
 */
interface YouTubeRepository {
    /** Open the YouTube app, or youtube.com as browser fallback. */
    suspend fun openYouTube(): CommandResult

    /** Open YouTube search results for [query]. */
    suspend fun search(query: String): CommandResult

    /** Open YouTube search for [query] with intent to play a video. */
    suspend fun playSearch(query: String): CommandResult

    /** Send a media-pause key event to the active media session. */
    suspend fun pause(): CommandResult

    /** Send a media-play key event to the active media session. */
    suspend fun resume(): CommandResult

    /** Send a media-stop key event to the active media session. */
    suspend fun stop(): CommandResult

    /** Send a media-next key event. */
    suspend fun next(): CommandResult

    /** Send a media-previous key event. */
    suspend fun previous(): CommandResult

    /** Increase media stream volume by one step. */
    suspend fun volumeUp(): CommandResult

    /** Decrease media stream volume by one step. */
    suspend fun volumeDown(): CommandResult

    /** Mute media stream volume. */
    suspend fun mute(): CommandResult

    /** Unmute media stream volume. */
    suspend fun unmute(): CommandResult

    /** Open YouTube home / landing page. */
    suspend fun openHome(): CommandResult

    /** Open YouTube Shorts feed. */
    suspend fun openShorts(): CommandResult

    /** Open YouTube Subscriptions feed. */
    suspend fun openSubscriptions(): CommandResult

    /** Open YouTube watch history. */
    suspend fun openHistory(): CommandResult

    /** Open the current user's YouTube channel page. */
    suspend fun openChannel(): CommandResult
}
