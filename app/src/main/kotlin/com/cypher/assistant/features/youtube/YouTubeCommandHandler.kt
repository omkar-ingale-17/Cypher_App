package com.cypher.assistant.features.youtube

import android.util.Log
import com.cypher.assistant.core.command.CommandHandler
import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.youtube.domain.repository.YouTubeRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "YouTubeCommandHandler"

/**
 * CommandHandler for Module 3: YouTube Control.
 *
 * Handles all YOUTUBE_* intents and delegates to [YouTubeRepository].
 * Does NOT own any microphone, TTS, or SpeechRecognizer state.
 * The returned [CommandResult.message] is spoken by Module 1's TTS engine.
 */
@Singleton
class YouTubeCommandHandler @Inject constructor(
    private val youTubeRepository: YouTubeRepository
) : CommandHandler {

    override val supportedIntents: Set<CommandIntentType> = setOf(
        CommandIntentType.YOUTUBE_SEARCH,
        CommandIntentType.YOUTUBE_PLAY_SEARCH,
        CommandIntentType.YOUTUBE_PAUSE,
        CommandIntentType.YOUTUBE_RESUME,
        CommandIntentType.YOUTUBE_STOP,
        CommandIntentType.YOUTUBE_NEXT,
        CommandIntentType.YOUTUBE_PREVIOUS,
        CommandIntentType.YOUTUBE_VOLUME_UP,
        CommandIntentType.YOUTUBE_VOLUME_DOWN,
        CommandIntentType.YOUTUBE_MUTE,
        CommandIntentType.YOUTUBE_UNMUTE,
        CommandIntentType.YOUTUBE_OPEN_HOME,
        CommandIntentType.YOUTUBE_OPEN_SHORTS,
        CommandIntentType.YOUTUBE_OPEN_SUBSCRIPTIONS,
        CommandIntentType.YOUTUBE_OPEN_HISTORY,
        CommandIntentType.YOUTUBE_OPEN_CHANNEL
    )

    override suspend fun handle(intent: CommandIntent): CommandResult {
        Log.i(TAG, "Handling YouTube intent: ${intent.intentType} params=${intent.parameters}")

        return when (intent.intentType) {
            CommandIntentType.YOUTUBE_SEARCH         -> handleSearch(intent)
            CommandIntentType.YOUTUBE_PLAY_SEARCH    -> handlePlaySearch(intent)
            CommandIntentType.YOUTUBE_PAUSE          -> youTubeRepository.pause()
            CommandIntentType.YOUTUBE_RESUME         -> youTubeRepository.resume()
            CommandIntentType.YOUTUBE_STOP           -> youTubeRepository.stop()
            CommandIntentType.YOUTUBE_NEXT           -> youTubeRepository.next()
            CommandIntentType.YOUTUBE_PREVIOUS       -> youTubeRepository.previous()
            CommandIntentType.YOUTUBE_VOLUME_UP      -> youTubeRepository.volumeUp()
            CommandIntentType.YOUTUBE_VOLUME_DOWN    -> youTubeRepository.volumeDown()
            CommandIntentType.YOUTUBE_MUTE           -> youTubeRepository.mute()
            CommandIntentType.YOUTUBE_UNMUTE         -> youTubeRepository.unmute()
            CommandIntentType.YOUTUBE_OPEN_HOME      -> youTubeRepository.openHome()
            CommandIntentType.YOUTUBE_OPEN_SHORTS    -> youTubeRepository.openShorts()
            CommandIntentType.YOUTUBE_OPEN_SUBSCRIPTIONS -> youTubeRepository.openSubscriptions()
            CommandIntentType.YOUTUBE_OPEN_HISTORY   -> youTubeRepository.openHistory()
            CommandIntentType.YOUTUBE_OPEN_CHANNEL   -> youTubeRepository.openChannel()
            else -> CommandResult.notYetImplemented(intent.intentType.name)
        }
    }

    private suspend fun handleSearch(intent: CommandIntent): CommandResult {
        val query = intent.parameters["query"]?.trim().orEmpty()
        if (query.isBlank()) {
            return CommandResult.failure("What would you like me to search for on YouTube?")
        }
        return youTubeRepository.search(query)
    }

    private suspend fun handlePlaySearch(intent: CommandIntent): CommandResult {
        val query = intent.parameters["query"]?.trim().orEmpty()
        if (query.isBlank()) {
            return CommandResult.failure("What would you like me to play on YouTube?")
        }
        return youTubeRepository.playSearch(query)
    }
}
