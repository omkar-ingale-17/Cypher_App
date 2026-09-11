package com.cypher.assistant.core.command

/**
 * The central command contract exchanged between every layer of Cypher.
 *
 * Produced by the NLU/parser layer and consumed by [CommandRouter] → [CommandHandler].
 *
 * @param intentType            The resolved intent category.
 * @param parameters            Feature-specific key/value pairs.
 *                              Keys are defined in [com.cypher.assistant.core.common.Constants.Params].
 * @param confidence            NLU confidence score (0.0–1.0). Values below 0.4 should be rejected.
 * @param requiresConfirmation  If true, the UI must ask the user to confirm before executing.
 * @param source                Origin of the command (voice, text, shortcut, etc.).
 * @param rawText               The original unprocessed user utterance.
 */
data class CommandIntent(
    val intentType: CommandIntentType,
    val parameters: Map<String, String> = emptyMap(),
    val confidence: Float = 1.0f,
    val requiresConfirmation: Boolean = false,
    val source: CommandSource = CommandSource.VOICE,
    val rawText: String = ""
) {
    /**
     * Returns the value for [key] in [parameters], or null if absent.
     * Prefer this for optional parameters.
     */
    fun param(key: String): String? = parameters[key]

    /**
     * Returns the value for [key] in [parameters], throwing [IllegalStateException]
     * if the key is missing. Use for parameters that must always be present.
     */
    fun requireParam(key: String): String =
        parameters[key] ?: error(
            "Required parameter '$key' not found in CommandIntent(${intentType.name}). " +
            "Available: ${parameters.keys}"
        )

    /** Returns true if this intent carries enough confidence to execute. */
    fun isConfident(threshold: Float = 0.4f): Boolean = confidence >= threshold

    companion object {
        /** Creates a typed UNKNOWN intent from raw text with zero confidence. */
        fun unknown(rawText: String, source: CommandSource = CommandSource.VOICE) = CommandIntent(
            intentType = CommandIntentType.UNKNOWN,
            rawText = rawText,
            confidence = 0f,
            source = source
        )
    }
}

/** Where the command originated. */
enum class CommandSource {
    VOICE,
    TEXT,
    SHORTCUT,
    AUTOMATED
}
