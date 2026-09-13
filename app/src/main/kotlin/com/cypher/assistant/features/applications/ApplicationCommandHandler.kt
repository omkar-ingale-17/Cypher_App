package com.cypher.assistant.features.applications

import android.util.Log
import com.cypher.assistant.core.command.CommandHandler
import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.applications.domain.model.LaunchResult
import com.cypher.assistant.features.applications.domain.repository.ApplicationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CommandHandler for Module 2: Application Control.
 * Handles OPEN_APP, CLOSE_APP, LIST_APPS, and OPEN_SETTINGS intents.
 */
@Singleton
class ApplicationCommandHandler @Inject constructor(
    private val applicationRepository: ApplicationRepository
) : CommandHandler {

    companion object {
        private const val TAG = "CYPHER_APP_HANDLER"
    }

    override val supportedIntents: Set<CommandIntentType> = setOf(
        CommandIntentType.OPEN_APP,
        CommandIntentType.CLOSE_APP,
        CommandIntentType.LIST_APPS,
        CommandIntentType.OPEN_SETTINGS
    )

    override suspend fun handle(intent: CommandIntent): CommandResult {
        Log.i(TAG, "Handling application control intent: ${intent.intentType} (params=${intent.parameters})")

        return when (intent.intentType) {
            CommandIntentType.OPEN_APP -> handleOpenApp(intent)
            CommandIntentType.CLOSE_APP -> handleCloseApp(intent)
            CommandIntentType.LIST_APPS -> handleListApps(intent)
            CommandIntentType.OPEN_SETTINGS -> handleOpenSettings(intent)
            else -> CommandResult.notYetImplemented(intent.intentType.name)
        }
    }

    private suspend fun handleOpenApp(intent: CommandIntent): CommandResult {
        val appNameQuery = intent.parameters["app_name"]?.trim().orEmpty()
        if (appNameQuery.isBlank()) {
            return CommandResult.failure("Which application would you like me to open?")
        }

        val launchResult = applicationRepository.openApplicationByName(appNameQuery)

        return when (launchResult) {
            is LaunchResult.Success -> {
                CommandResult.success(
                    message = launchResult.message,
                    data = mapOf(
                        "packageName" to launchResult.appInfo.packageName,
                        "appName" to launchResult.appInfo.appName
                    )
                )
            }

            is LaunchResult.Ambiguous -> {
                CommandResult.failure(launchResult.message)
            }

            is LaunchResult.NotFound -> {
                CommandResult.failure(launchResult.message)
            }

            is LaunchResult.Disabled -> {
                CommandResult.failure(launchResult.message)
            }

            is LaunchResult.Failed -> {
                CommandResult.failure(launchResult.message)
            }

            is LaunchResult.Restricted -> {
                CommandResult.failure(launchResult.message)
            }
        }
    }

    private fun handleCloseApp(intent: CommandIntent): CommandResult {
        val appName = intent.parameters["app_name"]?.trim().orEmpty()
        val targetName = if (appName.isNotBlank()) appName else "apps"
        // Truthful response in compliance with Android security sandbox restrictions
        return CommandResult.failure("Android doesn't allow me to force-stop $targetName from a normal assistant app.")
    }

    private suspend fun handleListApps(intent: CommandIntent): CommandResult {
        val apps = applicationRepository.getInstalledApplications()
        val sampleApps = apps.take(5).joinToString(", ") { it.appName }
        return CommandResult.success("You have ${apps.size} installed applications, including $sampleApps.")
    }

    private suspend fun handleOpenSettings(intent: CommandIntent): CommandResult {
        val settingType = intent.parameters["setting_type"] ?: "settings"
        val result = applicationRepository.openSystemScreen(settingType)
        return when (result) {
            is LaunchResult.Success -> CommandResult.success(result.message)
            else -> CommandResult.failure("I couldn't open settings.")
        }
    }
}
