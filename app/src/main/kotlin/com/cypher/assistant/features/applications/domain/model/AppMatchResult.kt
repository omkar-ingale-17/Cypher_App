package com.cypher.assistant.features.applications.domain.model

/**
 * Result of attempting to match a user query string against installed applications.
 */
sealed interface AppMatchResult {
    /** A confident single match was found. */
    data class Match(
        val appInfo: AppInfo,
        val confidence: Float,
        val matchedAlias: String? = null
    ) : AppMatchResult

    /** Multiple applications matched the query with comparable confidence. */
    data class Ambiguous(
        val query: String,
        val candidates: List<AppInfo>
    ) : AppMatchResult

    /** No installed application matched the query. */
    data class NotFound(
        val query: String
    ) : AppMatchResult
}
