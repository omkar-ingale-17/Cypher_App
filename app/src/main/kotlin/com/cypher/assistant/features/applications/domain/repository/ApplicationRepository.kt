package com.cypher.assistant.features.applications.domain.repository

import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.applications.domain.model.AppInfo
import com.cypher.assistant.features.applications.domain.model.AppMatchResult
import com.cypher.assistant.features.applications.domain.model.LaunchResult

/**
 * Contract for querying and launching installed applications and system navigation on the Android platform.
 */
interface ApplicationRepository {
    /** Returns all launchable user-facing applications installed on the device. */
    suspend fun getInstalledApplications(forceRefresh: Boolean = false): List<AppInfo>

    /** Searches installed applications by name or package query. */
    suspend fun searchApplications(query: String): List<AppInfo>

    /** Resolves an application from a natural language query using ApplicationMatcher. */
    suspend fun resolveApplication(query: String): AppMatchResult

    /** Launches an application by its package identifier. */
    suspend fun launchApplication(packageName: String): LaunchResult

    /** Resolves and launches an application in a single operation. */
    suspend fun openApplicationByName(appNameQuery: String): LaunchResult

    /** Opens a system screen or settings pane. */
    suspend fun openSystemScreen(screenType: String): LaunchResult

    /** Minimizes / navigates to the Android Home screen. */
    suspend fun launchHome(): LaunchResult

    /** Performs back navigation. */
    suspend fun goBack(): CommandResult

    /** Locks the device screen using supported Android APIs. */
    suspend fun lockScreen(): CommandResult

    /** Safely handles close app command within Android security boundaries. */
    suspend fun closeApp(appName: String): CommandResult
}
