package com.cypher.assistant.core.command

/**
 * Result returned by every [CommandHandler] after processing a [CommandIntent].
 *
 * The [message] is the text that the TTS engine will speak to the user.
 * [data] carries any structured data the UI layer might need (e.g., a list of apps).
 */
data class CommandResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any?> = emptyMap(),
    val error: Throwable? = null
) {
    companion object {
        /** Command executed successfully. */
        fun success(message: String, data: Map<String, Any?> = emptyMap()) =
            CommandResult(success = true, message = message, data = data)

        /** Command failed with a user-visible reason. */
        fun failure(message: String, error: Throwable? = null) =
            CommandResult(success = false, message = message, error = error)

        /** Used when a feature relies on an Android API that is restricted on this device/version. */
        fun restricted(feature: String, reason: String = "") = CommandResult(
            success = false,
            message = buildString {
                append("Sorry, I can't $feature on this device.")
                if (reason.isNotBlank()) append(" $reason")
            }
        )

        /** Used when a required runtime permission has not been granted. */
        fun permissionDenied(permission: String) = CommandResult(
            success = false,
            message = "I need permission to $permission. Please grant it in Settings."
        )

        /** Used when an intent is recognised but the handler is not yet implemented. */
        fun notYetImplemented(intentName: String) = CommandResult(
            success = false,
            message = "$intentName support is coming in a future update."
        )
    }
}
