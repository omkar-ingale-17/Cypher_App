package com.cypher.assistant.core.voice.wake

import java.util.Locale

/**
 * Result of evaluating raw transcript text for wake phrases.
 */
sealed interface WakePhraseResult {
    /** Only the wake phrase was detected (e.g., "Cypher"). */
    data object WakeOnly : WakePhraseResult

    /** Wake phrase detected along with an attached command payload (e.g., "Cypher, open WhatsApp"). */
    data class WakeWithCommand(val commandPayload: String) : WakePhraseResult

    /** No wake phrase prefix, raw direct command (e.g. user pressed mic button and said "open camera"). */
    data class DirectCommand(val commandPayload: String) : WakePhraseResult

    /** Empty or whitespace-only utterance. */
    data object None : WakePhraseResult
}

/**
 * Detects activation phrases and normalizes command text.
 *
 * Supported wake phrases:
 * - "cypher"
 * - "hey cypher"
 * - "ok cypher"
 * - "okay cypher"
 * - "hello cypher"
 */
class WakeWordDetector(
    private val wakePhrases: List<String> = listOf(
        "hey cypher",
        "ok cypher",
        "okay cypher",
        "hello cypher",
        "cypher"
    )
) {

    /**
     * Analyzes raw speech [input] and determines if a wake phrase was triggered.
     *
     * @param input Raw transcript from STT engine.
     * @param requireWakePhrase If true, inputs lacking a wake phrase are ignored (continuous mode).
     *                          If false, direct inputs are accepted as commands (push-to-talk mode).
     */
    fun process(input: String, requireWakePhrase: Boolean = false): WakePhraseResult {
        val normalized = normalizeText(input)
        if (normalized.isBlank()) return WakePhraseResult.None

        // Check for matching wake phrase prefixes (longest phrases first)
        for (phrase in wakePhrases.sortedByDescending { it.length }) {
            if (normalized == phrase) {
                return WakePhraseResult.WakeOnly
            }

            if (normalized.startsWith("$phrase ") || normalized.startsWith("$phrase,")) {
                val payload = normalized.removePrefix(phrase)
                    .trimStart(',', ' ', '.', '!', ':', ';', '?')
                    .trim()

                return if (payload.isNotBlank()) {
                    WakePhraseResult.WakeWithCommand(payload)
                } else {
                    WakePhraseResult.WakeOnly
                }
            }
        }

        return if (requireWakePhrase) {
            WakePhraseResult.None
        } else {
            WakePhraseResult.DirectCommand(normalized)
        }
    }

    /**
     * Cleans up raw transcription:
     * - Trims whitespace
     * - Replaces multiple spaces with single space
     * - Strips leading/trailing punctuation
     */
    fun normalizeText(raw: String): String {
        return raw.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""^[,\.\?!:;\s]+"""), "")
            .replace(Regex("""[,\.\?!:;\s]+$"""), "")
    }
}
