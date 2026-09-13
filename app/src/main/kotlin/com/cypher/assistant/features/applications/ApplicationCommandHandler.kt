package com.cypher.assistant.features.applications

import android.util.Log
import com.cypher.assistant.MainActivity
import com.cypher.assistant.core.command.CommandHandler
import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.applications.domain.model.LaunchResult
import com.cypher.assistant.features.applications.domain.repository.ApplicationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CommandHandler for Module 2: Application Control & System Navigation.
 * Handles OPEN_APP, CLOSE_APP, MINIMIZE_APP, GO_HOME, LOCK_SCREEN, LIST_APPS, and OPEN_SETTINGS intents.
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
        CommandIntentType.MINIMIZE_APP,
        CommandIntentType.GO_HOME,
        CommandIntentType.LOCK_SCREEN,
        CommandIntentType.LIST_APPS,
        CommandIntentType.OPEN_SETTINGS
    )

    override suspend fun handle(intent: CommandIntent): CommandResult {
        Log.i(TAG, "Handling application control intent: ${intent.intentType} (params=${intent.parameters})")

        return when (intent.intentType) {
            CommandIntentType.OPEN_APP -> handleOpenApp(intent)
            CommandIntentType.CLOSE_APP -> handleCloseApp(intent)
            CommandIntentType.MINIMIZE_APP -> handleMinimize(intent)
            CommandIntentType.GO_HOME -> handleGoHome(intent)
            CommandIntentType.LOCK_SCREEN -> handleLockScreen(intent)
            CommandIntentType.LIST_APPS -> handleListApps(intent)
            CommandIntentType.OPEN_SETTINGS -> handleOpenSettings(intent)
            else -> CommandResult.notYetImplemented(intent.intentType.name)
        }
    }

    private suspend fun handleOpenApp(intent: CommandIntent): CommandResult {
        val appNameQuery = intent.parameters["app_name"]?.trim().orEmpty()
        Log.i("CYPHER_COMMAND", "[CYPHER_COMMAND] Action: openApplicationByName (\"$appNameQuery\")")
        if (appNameQuery.isBlank()) {
            return CommandResult.failure("Which application would you like me to open?")
        }

        val launchResult = applicationRepository.openApplicationByName(appNameQuery)

        return when (launchResult) {
            is LaunchResult.Success -> {
                Log.i("CYPHER_COMMAND", "[CYPHER_COMMAND] Action: Successfully launched ${launchResult.appInfo.appName}")
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

    private suspend fun handleCloseApp(intent: CommandIntent): CommandResult {
        val appName = intent.parameters["app_name"]?.trim().orEmpty()
        Log.i("CYPHER_COMMAND", "[CYPHER_COMMAND] Action: closeApp (\"$appName\")")
        return applicationRepository.closeApp(appName)
    }

    private suspend fun handleMinimize(intent: CommandIntent): CommandResult {
        Log.i("CYPHER_COMMAND", "[CYPHER_COMMAND] Action: moveTaskToBack")
        val minimizedViaActivity = MainActivity.minimizeActivity()
        if (!minimizedViaActivity) {
            applicationRepository.launchHome()
        }
        return CommandResult.success("Minimizing.")
    }

    private suspend fun handleGoHome(intent: CommandIntent): CommandResult {
        Log.i("CYPHER_COMMAND", "[CYPHER_COMMAND] Action: launchHome (Intent.CATEGORY_HOME)")
        val result = applicationRepository.launchHome()
        return when (result) {
            is LaunchResult.Success -> CommandResult.success("Going to home screen.")
            else -> CommandResult.failure("I couldn't return to the home screen.")
        }
    }

    private suspend fun handleLockScreen(intent: CommandIntent): CommandResult {
        Log.i("CYPHER_COMMAND", "[CYPHER_COMMAND] Action: lockScreen (AccessibilityService / DevicePolicyManager)")
        return applicationRepository.lockScreen()
    }

    private suspend fun handleListApps(intent: CommandIntent): CommandResult {
        Log.i("CYPHER_COMMAND", "[CYPHER_COMMAND] Action: getInstalledApplications")
        val apps = applicationRepository.getInstalledApplications()
        val sampleApps = apps.take(5).joinToString(", ") { it.appName }
        return CommandResult.success("You have ${apps.size} installed applications, including $sampleApps.")
    }

    private suspend fun handleOpenSettings(intent: CommandIntent): CommandResult {
        val settingType = intent.parameters["setting_type"] ?: "settings"
        Log.i("CYPHER_COMMAND", "[CYPHER_COMMAND] Action: openSystemScreen (\"$settingType\")")
        val result = applicationRepository.openSystemScreen(settingType)
        return when (result) {
            is LaunchResult.Success -> CommandResult.success(result.message)
            else -> CommandResult.failure("I couldn't open settings.")
        }
    }
}
