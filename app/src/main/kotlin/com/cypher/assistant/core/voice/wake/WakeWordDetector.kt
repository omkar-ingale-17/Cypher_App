package com.cypher.assistant.core.voice.wake

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of evaluating raw speech transcript for wake words and command payloads.
 */
sealed interface WakePhraseResult {
    /** Only the wake phrase was detected (e.g. "Cypher", "Hey Cypher", "Jaan", "Baby"). */
    data class WakeOnly(val wakeWord: String) : WakePhraseResult

    /** Wake phrase detected along with an attached command payload (e.g. "Cypher, what is the time", "What is the time Cypher"). */
    data class WakeWithCommand(val wakeWord: String, val commandPayload: String) : WakePhraseResult

    /** Direct command without wake word (used in manual push-to-talk mic mode). */
    data class DirectCommand(val commandPayload: String) : WakePhraseResult

    /** No wake phrase detected in standby mode or empty transcript. */
    data object None : WakePhraseResult
}

/**
 * Strongly typed result representing wake word and extracted command.
 */
data class WakeWordResult(
    val wakeWord: String?,
    val command: String?
)

/**
 * Centralized multi-position wake-word detector for Cypher AI.
 *
 * Supported official wake words:
 * - "cypher" / "cipher"
 * - "jan" / "jaan" / "jann"
 * - "baby"
 *
 * Supports wake words anywhere in the utterance:
 * - Start: "Cypher what is the time", "Hey Cypher open YouTube"
 * - End: "What is the time Cypher", "Play music baby", "Tell me a joke jaan"
 * - Standalone: "Cypher", "Jaan", "Baby", "Hey Cypher"
 */
@Singleton
class WakeWordDetector @Inject constructor() {

    private val baseWakeWords: List<String> = listOf(
        "cypher",
        "cipher",
        "jaan",
        "jann",
        "jan",
        "baby"
    )

    private val prefixModifiers: List<String> = listOf(
        "hey",
        "ok",
        "okay",
        "hello",
        "hi",
        "yo",
        "dear"
    )

    private val suffixModifiers: List<String> = listOf(
        "listen",
        "please"
    )

    /**
     * Checks if the given [transcript] contains any official wake word.
     * Returns the canonical wake word ("cypher", "jaan", "baby") if found, or null.
     */
    fun containsWakeWord(transcript: String): String? {
        return extractWakeWordAndCommand(transcript).wakeWord
    }

    /**
     * Core extraction function: finds the wake word anywhere in the transcript,
     * removes it along with any surrounding filler words, and extracts the command.
     */
    fun extractWakeWordAndCommand(transcript: String): WakeWordResult {
        val normalized = normalizeText(transcript)
        if (normalized.isBlank()) {
            return WakeWordResult(wakeWord = null, command = null)
        }

        // Build list of candidate wake word expressions, prioritizing longer phrases ("hey cypher" before "cypher")
        val candidatePhrases = mutableListOf<Pair<String, String>>() // Pair(phraseToMatch, canonicalWakeWord)

        for (prefix in prefixModifiers) {
            for (word in baseWakeWords) {
                candidatePhrases.add(Pair("$prefix $word", canonicalizeWakeWord(word)))
            }
        }
        for (word in baseWakeWords) {
            candidatePhrases.add(Pair(word, canonicalizeWakeWord(word)))
        }

        // Sort candidate phrases by length descending to match compound phrases first
        val sortedCandidates = candidatePhrases.distinctBy { it.first }.sortedByDescending { it.first.length }

        for ((phrase, canonical) in sortedCandidates) {
            // Find phrase with word boundaries
            val regex = Regex("""(?:\b|^)${Regex.escape(phrase)}(?:\b|$)""", RegexOption.IGNORE_CASE)
            val match = regex.find(normalized)

            if (match != null) {
                // Phrase found at match.range
                val before = normalized.substring(0, match.range.first).trim()
                val after = normalized.substring(match.range.last + 1).trim()

                // Combine remaining text before and after the wake phrase
                var remaining = when {
                    before.isNotBlank() && after.isNotBlank() -> "$before $after"
                    before.isNotBlank() -> before
                    after.isNotBlank() -> after
                    else -> ""
                }

                // Clean up leading/trailing filler words like "please", "listen"
                remaining = cleanFillers(remaining)

                val finalCommand = remaining.ifBlank { null }
                return WakeWordResult(
                    wakeWord = canonical,
                    command = finalCommand
                )
            }
        }

        return WakeWordResult(wakeWord = null, command = null)
    }

    /**
     * Analyzes raw speech [input] and determines whether a wake word or command is present.
     */
    fun process(input: String, requireWakePhrase: Boolean = false): WakePhraseResult {
        val (wakeWord, command) = extractWakeWordAndCommand(input)

        if (wakeWord != null) {
            return if (command != null) {
                WakePhraseResult.WakeWithCommand(
                    wakeWord = wakeWord,
                    commandPayload = command
                )
            } else {
                WakePhraseResult.WakeOnly(wakeWord = wakeWord)
            }
        }

        val normalized = normalizeText(input)
        return if (requireWakePhrase) {
            WakePhraseResult.None
        } else {
            if (normalized.isNotBlank()) {
                WakePhraseResult.DirectCommand(normalized)
            } else {
                WakePhraseResult.None
            }
        }
    }

    /**
     * Strips leading/trailing conversational filler words and punctuation.
     */
    private fun cleanFillers(text: String): String {
        var result = text.trim()

        // Strip leading prefixes if any left (e.g. "hey")
        for (prefix in prefixModifiers) {
            val prefixRegex = Regex("""^(?:$prefix)\b[\s,\.\?!:;-]*""", RegexOption.IGNORE_CASE)
            result = prefixRegex.replace(result, "")
        }

        // Strip leading/trailing suffixes (e.g. "please", "listen")
        for (suffix in suffixModifiers) {
            val suffixLeadRegex = Regex("""^(?:$suffix)\b[\s,\.\?!:;-]*""", RegexOption.IGNORE_CASE)
            result = suffixLeadRegex.replace(result, "")
            val suffixTrailRegex = Regex("""[\s,\.\?!:;-]*\b(?:$suffix)$""", RegexOption.IGNORE_CASE)
            result = suffixTrailRegex.replace(result, "")
        }

        return result.trim(' ', ',', '.', '?', '!', ':', ';', '-')
    }

    /**
     * Map variations (e.g. "cipher", "jann") to their canonical wake word representation.
     */
    fun canonicalizeWakeWord(word: String): String {
        return when (word.lowercase(Locale.ROOT)) {
            "cipher", "cypher" -> "cypher"
            "jann", "jaan", "jan" -> "jaan"
            "baby" -> "baby"
            else -> word.lowercase(Locale.ROOT)
        }
    }

    /**
     * Cleans up and normalizes raw text for robust matching.
     */
    fun normalizeText(raw: String): String {
        return raw.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("""[,\.\?!:;\-_]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
