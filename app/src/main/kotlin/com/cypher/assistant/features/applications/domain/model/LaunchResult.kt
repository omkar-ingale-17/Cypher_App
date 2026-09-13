package com.cypher.assistant.features.applications.domain.model

/**
 * Result of attempting to launch an application or screen.
 */
sealed interface LaunchResult {
    data class Success(val appInfo: AppInfo, val message: String) : LaunchResult
    data class NotFound(val query: String, val message: String) : LaunchResult
    data class Ambiguous(val candidates: List<AppInfo>, val message: String) : LaunchResult
    data class Disabled(val appInfo: AppInfo, val message: String) : LaunchResult
    data class Failed(val appName: String, val error: String, val message: String) : LaunchResult
    data class Restricted(val message: String) : LaunchResult
}
