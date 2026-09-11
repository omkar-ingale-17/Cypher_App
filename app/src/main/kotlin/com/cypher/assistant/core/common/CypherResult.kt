package com.cypher.assistant.core.common

/**
 * Generic sealed result type used across all layers of Cypher.
 *
 * Prefer returning [CypherResult] from repository and use-case functions rather
 * than throwing exceptions, so callers can handle every case explicitly.
 */
sealed class CypherResult<out T> {

    /** The operation completed successfully and returned [data]. */
    data class Success<T>(val data: T) : CypherResult<T>()

    /** The operation failed with a human-readable [message] and optional [throwable]. */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : CypherResult<Nothing>()

    /** The operation is in progress (used for UI state). */
    data object Loading : CypherResult<Nothing>()
}

/** Maps the [data] inside a [CypherResult.Success] to a new type, leaving errors untouched. */
inline fun <T, R> CypherResult<T>.map(transform: (T) -> R): CypherResult<R> = when (this) {
    is CypherResult.Success -> CypherResult.Success(transform(data))
    is CypherResult.Error   -> this
    is CypherResult.Loading -> this
}

/** Returns the data if [Success], or null otherwise. */
fun <T> CypherResult<T>.getOrNull(): T? =
    (this as? CypherResult.Success)?.data

/** Returns true only for [CypherResult.Success]. */
val CypherResult<*>.isSuccess get() = this is CypherResult.Success

/** Returns true only for [CypherResult.Error]. */
val CypherResult<*>.isError get() = this is CypherResult.Error
