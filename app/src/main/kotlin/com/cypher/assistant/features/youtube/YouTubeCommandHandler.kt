package com.cypher.assistant.features.youtube

import android.util.Log
import com.cypher.assistant.core.command.CommandHandler
import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.youtube.domain.model.YouTubeActionResult
import com.cypher.assistant.features.youtube.domain.model.YouTubeCommand
import com.cypher.assistant.features.youtube.domain.repository.YouTubeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CommandHandler for Module 3: Complete YouTube App Control.
 * Routes all YOUTUBE_* intents to the [YouTubeRepository] and provides verified user-facing messages.
 */
@Singleton
class YouTubeCommandHandler @Inject constructor(
    private val repository: YouTubeRepository
) : CommandHandler {

    companion object {
        private const val TAG = "YouTubeCommandHandler"
    }

    override val supportedIntents: Set<CommandIntentType> = setOf(
        CommandIntentType.YOUTUBE_SEARCH,
        CommandIntentType.YOUTUBE_PLAY_SEARCH,
        CommandIntentType.YOUTUBE_PLAY_INDEX,
        CommandIntentType.YOUTUBE_PAUSE,
        CommandIntentType.YOUTUBE_RESUME,
        CommandIntentType.YOUTUBE_STOP,
        CommandIntentType.YOUTUBE_NEXT,
        CommandIntentType.YOUTUBE_PREVIOUS,
        CommandIntentType.YOUTUBE_LIKE,
        CommandIntentType.YOUTUBE_DISLIKE,
        CommandIntentType.YOUTUBE_SUBSCRIBE,
        CommandIntentType.YOUTUBE_UNSUBSCRIBE,
        CommandIntentType.YOUTUBE_COMMENTS_OPEN,
        CommandIntentType.YOUTUBE_COMMENTS_CLOSE,
        CommandIntentType.YOUTUBE_DESCRIPTION_OPEN,
        CommandIntentType.YOUTUBE_SHOW_MORE,
        CommandIntentType.YOUTUBE_SHOW_LESS,
        CommandIntentType.YOUTUBE_SCROLL_UP,
        CommandIntentType.YOUTUBE_SCROLL_DOWN,
        CommandIntentType.YOUTUBE_OPEN_HOME,
        CommandIntentType.YOUTUBE_OPEN_SHORTS,
        CommandIntentType.YOUTUBE_OPEN_SUBSCRIPTIONS,
        CommandIntentType.YOUTUBE_OPEN_HISTORY,
        CommandIntentType.YOUTUBE_OPEN_CHANNEL,
        CommandIntentType.YOUTUBE_OPEN_NOTIFICATIONS,
        CommandIntentType.YOUTUBE_GO_BACK,
        CommandIntentType.YOUTUBE_VOLUME_UP,
        CommandIntentType.YOUTUBE_VOLUME_DOWN,
        CommandIntentType.YOUTUBE_MUTE,
        CommandIntentType.YOUTUBE_UNMUTE
    )

    override suspend fun handle(intent: CommandIntent): CommandResult {
        Log.i(TAG, "Handling YouTube intent: \${intent.intentType} (params=\${intent.parameters})")

        val command: YouTubeCommand = when (intent.intentType) {
            CommandIntentType.YOUTUBE_SEARCH -> {
                val query = intent.parameters["query"]?.trim().orEmpty()
                YouTubeCommand.Search(query)
            }
            CommandIntentType.YOUTUBE_PLAY_SEARCH -> {
                val query = intent.parameters["query"]?.trim().orEmpty()
                YouTubeCommand.PlaySearch(query)
            }
            CommandIntentType.YOUTUBE_PLAY_INDEX -> {
                val index = intent.parameters["index"]?.toIntOrNull() ?: 1
                YouTubeCommand.PlayIndex(index)
            }
            CommandIntentType.YOUTUBE_PAUSE -> YouTubeCommand.Pause
            CommandIntentType.YOUTUBE_RESUME -> YouTubeCommand.Resume
            CommandIntentType.YOUTUBE_STOP -> YouTubeCommand.Stop
            CommandIntentType.YOUTUBE_NEXT -> YouTubeCommand.Next
            CommandIntentType.YOUTUBE_PREVIOUS -> YouTubeCommand.Previous
            CommandIntentType.YOUTUBE_LIKE -> YouTubeCommand.Like
            CommandIntentType.YOUTUBE_DISLIKE -> YouTubeCommand.Dislike
            CommandIntentType.YOUTUBE_SUBSCRIBE -> YouTubeCommand.Subscribe
            CommandIntentType.YOUTUBE_UNSUBSCRIBE -> {
                val confirmed = intent.parameters["confirmed"]?.toBoolean() ?: false
                YouTubeCommand.Unsubscribe(confirmed)
            }
            CommandIntentType.YOUTUBE_COMMENTS_OPEN -> YouTubeCommand.OpenComments
            CommandIntentType.YOUTUBE_COMMENTS_CLOSE -> YouTubeCommand.CloseComments
            CommandIntentType.YOUTUBE_DESCRIPTION_OPEN -> YouTubeCommand.OpenDescription
            CommandIntentType.YOUTUBE_SHOW_MORE -> YouTubeCommand.ShowMore
            CommandIntentType.YOUTUBE_SHOW_LESS -> YouTubeCommand.ShowLess
            CommandIntentType.YOUTUBE_SCROLL_UP -> YouTubeCommand.ScrollUp
            CommandIntentType.YOUTUBE_SCROLL_DOWN -> YouTubeCommand.ScrollDown
            CommandIntentType.YOUTUBE_OPEN_HOME -> YouTubeCommand.OpenHome
            CommandIntentType.YOUTUBE_OPEN_SHORTS -> YouTubeCommand.OpenShorts
            CommandIntentType.YOUTUBE_OPEN_SUBSCRIPTIONS -> YouTubeCommand.OpenSubscriptions
            CommandIntentType.YOUTUBE_OPEN_HISTORY -> YouTubeCommand.OpenHistory
            CommandIntentType.YOUTUBE_OPEN_CHANNEL -> YouTubeCommand.OpenChannel
            CommandIntentType.YOUTUBE_OPEN_NOTIFICATIONS -> YouTubeCommand.OpenNotifications
            CommandIntentType.YOUTUBE_GO_BACK -> YouTubeCommand.GoBack
            CommandIntentType.YOUTUBE_VOLUME_UP -> YouTubeCommand.VolumeUp
            CommandIntentType.YOUTUBE_VOLUME_DOWN -> YouTubeCommand.VolumeDown
            CommandIntentType.YOUTUBE_MUTE -> YouTubeCommand.Mute
            CommandIntentType.YOUTUBE_UNMUTE -> YouTubeCommand.Unmute
            else -> return CommandResult.notYetImplemented(intent.intentType.name)
        }

        val result = repository.execute(command)
        return mapActionResultToCommandResult(result)
    }

    private fun mapActionResultToCommandResult(result: YouTubeActionResult): CommandResult {
        return when (result) {
            is YouTubeActionResult.Success -> CommandResult.success(result.message)
            is YouTubeActionResult.AlreadyInState -> CommandResult.success(result.message)
            is YouTubeActionResult.NotFound -> CommandResult.failure(result.reason)
            is YouTubeActionResult.NotAvailable -> CommandResult.failure(result.reason)
            is YouTubeActionResult.PermissionRequired -> CommandResult.failure(result.reason)
            is YouTubeActionResult.ConfirmationRequired -> CommandResult.success(
                message = result.message,
                data = mapOf("requiresConfirmation" to true)
            )
            is YouTubeActionResult.Failed -> CommandResult.failure(result.reason)
            is YouTubeActionResult.Timeout -> CommandResult.failure("YouTube did not respond in time.")
        }
    }
}
