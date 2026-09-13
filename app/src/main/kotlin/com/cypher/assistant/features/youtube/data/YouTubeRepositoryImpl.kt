package com.cypher.assistant.features.youtube.data

import android.view.KeyEvent
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.youtube.domain.repository.YouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [YouTubeRepository].
 *
 * All Android Intent / AudioManager calls are dispatched on [Dispatchers.Main]
 * because startActivity() must run on the main thread.
 */
@Singleton
class YouTubeRepositoryImpl @Inject constructor(
    private val dataSource: YouTubeIntentDataSource
) : YouTubeRepository {

    override suspend fun openYouTube(): CommandResult = withContext(Dispatchers.Main) {
        val launched = dataSource.launchYouTubeApp()
        if (launched) {
            CommandResult.success("Opening YouTube.")
        } else {
            CommandResult.failure("I couldn't open YouTube.")
        }
    }

    override suspend fun search(query: String): CommandResult = withContext(Dispatchers.Main) {
        if (query.isBlank()) {
            return@withContext CommandResult.failure("What would you like me to search for on YouTube?")
        }
        val opened = dataSource.openSearchUrl(query)
        if (opened) {
            CommandResult.success(
                message = "Searching YouTube for $query.",
                data = mapOf("query" to query)
            )
        } else {
            CommandResult.failure("I couldn't open YouTube search right now.")
        }
    }

    override suspend fun playSearch(query: String): CommandResult = withContext(Dispatchers.Main) {
        if (query.isBlank()) {
            return@withContext CommandResult.failure("What would you like me to search for on YouTube?")
        }
        // We can open a search — we cannot guarantee playback selection without private APIs.
        val opened = dataSource.openSearchUrl(query)
        if (opened) {
            CommandResult.success(
                // Honest message: we opened search, not guaranteed playback
                message = "Searching YouTube for $query.",
                data = mapOf("query" to query)
            )
        } else {
            CommandResult.failure("I couldn't open YouTube.")
        }
    }

    override suspend fun pause(): CommandResult = withContext(Dispatchers.Main) {
        dataSource.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
        CommandResult.success("Paused.")
    }

    override suspend fun resume(): CommandResult = withContext(Dispatchers.Main) {
        dataSource.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
        CommandResult.success("Resuming playback.")
    }

    override suspend fun stop(): CommandResult = withContext(Dispatchers.Main) {
        dataSource.sendMediaKey(KeyEvent.KEYCODE_MEDIA_STOP)
        CommandResult.success("Stopped.")
    }

    override suspend fun next(): CommandResult = withContext(Dispatchers.Main) {
        dataSource.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        CommandResult.success("Skipping to the next video.")
    }

    override suspend fun previous(): CommandResult = withContext(Dispatchers.Main) {
        dataSource.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        CommandResult.success("Going back to the previous video.")
    }

    override suspend fun volumeUp(): CommandResult = withContext(Dispatchers.Main) {
        dataSource.volumeUp()
        CommandResult.success("Volume increased.")
    }

    override suspend fun volumeDown(): CommandResult = withContext(Dispatchers.Main) {
        dataSource.volumeDown()
        CommandResult.success("Volume decreased.")
    }

    override suspend fun mute(): CommandResult = withContext(Dispatchers.Main) {
        dataSource.mute()
        CommandResult.success("Muted.")
    }

    override suspend fun unmute(): CommandResult = withContext(Dispatchers.Main) {
        dataSource.unmute()
        CommandResult.success("Unmuted.")
    }

    override suspend fun openHome(): CommandResult = withContext(Dispatchers.Main) {
        val opened = dataSource.launchYouTubeApp()
        if (opened) CommandResult.success("Opening YouTube.")
        else CommandResult.failure("I couldn't open YouTube.")
    }

    override suspend fun openShorts(): CommandResult = withContext(Dispatchers.Main) {
        val opened = dataSource.openYouTubeDeepLink("shorts")
        if (opened) CommandResult.success("Opening YouTube Shorts.")
        else CommandResult.failure("I couldn't open YouTube Shorts.")
    }

    override suspend fun openSubscriptions(): CommandResult = withContext(Dispatchers.Main) {
        val opened = dataSource.openYouTubeDeepLink("feed/subscriptions")
        if (opened) CommandResult.success("Opening your YouTube subscriptions.")
        else CommandResult.failure("I couldn't open YouTube subscriptions.")
    }

    override suspend fun openHistory(): CommandResult = withContext(Dispatchers.Main) {
        val opened = dataSource.openYouTubeDeepLink("feed/history")
        if (opened) CommandResult.success("Opening your YouTube watch history.")
        else CommandResult.failure("I couldn't open YouTube history.")
    }

    override suspend fun openChannel(): CommandResult = withContext(Dispatchers.Main) {
        // Opens youtube.com/channel/me — redirects to logged-in user's channel
        val opened = dataSource.openUrl("https://www.youtube.com/channel/me")
        if (opened) CommandResult.success("Opening your YouTube channel.")
        else CommandResult.failure("I couldn't open your YouTube channel.")
    }
}
