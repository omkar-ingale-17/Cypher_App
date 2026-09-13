package com.cypher.assistant.features.conversation

import com.cypher.assistant.core.command.CommandHandler
import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.data.preferences.UserPreferencesDataStore
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationHandler @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore
) : CommandHandler {

    override val supportedIntents: Set<CommandIntentType> = setOf(
        CommandIntentType.GREETING,
        CommandIntentType.ASSISTANT_NAME,
        CommandIntentType.ASSISTANT_STATUS,
        CommandIntentType.ASSISTANT_CAPABILITIES,
        CommandIntentType.SET_USER_NAME,
        CommandIntentType.GET_USER_NAME,
        CommandIntentType.GET_TIME,
        CommandIntentType.GET_DATE,
        CommandIntentType.THANK_YOU,
        CommandIntentType.GOODBYE
    )

    override suspend fun handle(intent: CommandIntent): CommandResult {
        val userName = preferencesDataStore.userPreferences.first().userName
        val greetingName = if (userName.isNotBlank() && userName != "Commander") userName else "there"

        return when (intent.intentType) {
            CommandIntentType.GREETING -> {
                CommandResult.success("Hello $greetingName! How can I assist you today?")
            }

            CommandIntentType.ASSISTANT_NAME -> {
                CommandResult.success("My name is Cypher, your personal Android voice assistant.")
            }

            CommandIntentType.ASSISTANT_STATUS -> {
                CommandResult.success("I'm running at peak performance! How can I help you, $greetingName?")
            }

            CommandIntentType.ASSISTANT_CAPABILITIES -> {
                CommandResult.success("I can tell you the time and date, open apps, make calls, send SMS messages, search YouTube, adjust volume and settings, search the web, and chat with you.")
            }

            CommandIntentType.SET_USER_NAME -> {
                val newName = intent.parameters["name"]?.trim().orEmpty()
                if (newName.isNotBlank()) {
                    preferencesDataStore.setUserName(newName)
                    CommandResult.success("Nice to meet you, $newName! I will remember your name.")
                } else {
                    CommandResult.failure("I didn't catch your name. You can say 'My name is John'.")
                }
            }

            CommandIntentType.GET_USER_NAME -> {
                if (userName.isNotBlank() && userName != "Commander") {
                    CommandResult.success("Your name is $userName.")
                } else {
                    CommandResult.success("I don't know your name yet. You can tell me by saying 'My name is...', or set it in settings.")
                }
            }

            CommandIntentType.GET_TIME -> {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val currentTime = timeFormat.format(Date())
                CommandResult.success("The time is $currentTime.")
            }

            CommandIntentType.GET_DATE -> {
                val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                val currentDate = dateFormat.format(Date())
                CommandResult.success("Today is $currentDate.")
            }

            CommandIntentType.THANK_YOU -> {
                CommandResult.success("You're very welcome, $greetingName!")
            }

            CommandIntentType.GOODBYE -> {
                CommandResult.success("Goodbye $greetingName! Say 'Cypher' whenever you need me.")
            }

            else -> CommandResult.notYetImplemented(intent.intentType.name)
        }
    }
}
