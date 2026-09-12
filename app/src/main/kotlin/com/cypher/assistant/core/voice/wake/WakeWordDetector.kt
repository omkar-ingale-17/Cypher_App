package com.cypher.assistant.core.voice.wake

import java.util.Locale

/**
 * Result of evaluating raw speech transcript for wake words and command payloads.
 */
sealed interface WakePhraseResult {
    /** Only the wake phrase was detected (e.g. "Cypher", "Hey Cypher", "Jaan", "Baby"). */
    data class WakeOnly(val wakeWord: String) : WakePhraseResult

    /** Wake phrase detected along with an attached command payload (e.g. "Cypher, what is the time"). */
    data class WakeWithCommand(val wakeWord: String, val commandPayload: String) : WakePhraseResult

    /** Direct command without wake word (used in manual push-to-talk mic mode). */
    data class DirectCommand(val commandPayload: String) : WakePhraseResult

    /** No wake phrase detected in standby mode or empty transcript. */
    data object None : WakePhraseResult
}

/**
 * Centralized wake-word detector for Cypher AI.
 *
 * Supported official wake words and transcription variations:
 * - "cypher" / "cipher"
 * - "jan" / "jaan" / "jann"
 * - "baby"
 *
 * Supports conversational prefix variants:
 * - "hey cypher", "ok cypher", "okay cypher", "hello cypher", "cypher listen"
 * - "hey baby", "ok baby"
 * - "hey jan", "hey jaan", "hello jaan"
 */
class WakeWordDetector(
    private val baseWakeWords: List<String> = listOf(
        "cypher",
        "cipher",
        "jaan",
        "jann",
        "jan",
        "baby"
    ),
    private val prefixModifiers: List<String> = listOf(
        "hey",
        "ok",
        "okay",
        "hello",
        "hi",
        "dear"
    ),
    private val suffixModifiers: List<String> = listOf(
        "listen",
        "please",
        "can you hear me"
    )
) {

    /**
     * Checks if the given [transcript] contains any official wake word or wake phrase.
     * Returns the normalized base wake word (e.g. "cypher", "jan", "jaan", "baby") if found, or null.
     */
    fun containsWakeWord(transcript: String): String? {
        val normalized = normalizeText(transcript)
        if (normalized.isBlank()) return null

        // 1. Direct check against compound phrases first (e.g. "hey cypher", "ok baby")
        for (prefix in prefixModifiers) {
            for (word in baseWakeWords) {
                val phrase = "$prefix $word"
                if (hasWordOrPhrase(normalized, phrase)) {
                    return canonicalizeWakeWord(word)
                }
            }
        }

        // 2. Direct check against base wake words
        for (word in baseWakeWords) {
            if (hasWordOrPhrase(normalized, word)) {
                return canonicalizeWakeWord(word)
            }
        }

        return null
    }

    /**
     * Analyzes raw speech [input] and determines whether a wake word or command is present.
     *
     * @param input Raw transcript from speech recognizer.
     * @param requireWakePhrase If true (Stage 1 / Standby), input without wake word is ignored.
     *                          If false (Stage 2 / Manual Mic), input is treated as a direct command.
     */
    fun process(input: String, requireWakePhrase: Boolean = false): WakePhraseResult {
        val normalized = normalizeText(input)
        if (normalized.isBlank()) return WakePhraseResult.None

        val detectedWord = containsWakeWord(normalized)

        if (detectedWord != null) {
            val commandPayload = extractCommand(normalized, detectedWord)
            return if (commandPayload.isNotBlank()) {
                WakePhraseResult.WakeWithCommand(
                    wakeWord = detectedWord,
                    commandPayload = commandPayload
                )
            } else {
                WakePhraseResult.WakeOnly(wakeWord = detectedWord)
            }
        }

        return if (requireWakePhrase) {
            WakePhraseResult.None
        } else {
            WakePhraseResult.DirectCommand(normalized)
        }
    }

    /**
     * Extracts the remaining command after removing the detected wake word and any associated prefixes/fillers.
     * E.g.:
     * - "hey cypher what is the time" -> "what is the time"
     * - "cypher, open camera please" -> "open camera please"
     * - "baby tell me a joke" -> "tell me a joke"
     */
    fun extractCommand(transcript: String, detectedWakeWord: String): String {
        val normalized = normalizeText(transcript)
        if (normalized.isBlank()) return ""

        // Build list of all potential wake phrase patterns to strip from the beginning
        val phrasesToStrip = mutableListOf<String>()

        for (prefix in prefixModifiers) {
            phrasesToStrip.add("$prefix $detectedWakeWord")
            for (base in baseWakeWords) {
                phrasesToStrip.add("$prefix $base")
            }
        }
        phrasesToStrip.add(detectedWakeWord)
        for (base in baseWakeWords) {
            phrasesToStrip.add(base)
        }

        // Sort descending by length so longer phrases ("hey cypher") are stripped before ("cypher")
        var remainder = normalized
        for (phrase in phrasesToStrip.distinct().sortedByDescending { it.length }) {
            val regex = Regex("""^(?:$phrase)\b[\s,\.\?!:;-]*""", RegexOption.IGNORE_CASE)
            val match = regex.find(remainder)
            if (match != null) {
                remainder = remainder.substring(match.range.last + 1).trim()
                break
            }
        }

        // Also clean up any leading suffix modifiers like "listen", "please" if present at start of command
        for (suffix in suffixModifiers) {
            val suffixRegex = Regex("""^(?:$suffix)\b[\s,\.\?!:;-]*""", RegexOption.IGNORE_CASE)
            val match = suffixRegex.find(remainder)
            if (match != null) {
                remainder = remainder.substring(match.range.last + 1).trim()
            }
        }

        return remainder.trimStart(',', ' ', '.', '!', ':', ';', '?', '-').trim()
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
     * Checks if the text contains a specific word or phrase bounded by word boundaries or punctuation.
     */
    private fun hasWordOrPhrase(text: String, phrase: String): Boolean {
        val pattern = Regex("""(?:\b|^)${Regex.escape(phrase)}(?:\b|$)""", RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(text)
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
