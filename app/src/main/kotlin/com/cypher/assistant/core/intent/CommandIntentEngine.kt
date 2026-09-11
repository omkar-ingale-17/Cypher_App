package com.cypher.assistant.core.intent

import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandSource
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses normalized natural-language text into structured [CommandIntent]s.
 *
 * Implements rule-based, regex-driven intent classification for deterministic
 * commands (no internet/cloud dependency required). Extensible for cloud LLMs.
 */
@Singleton
class CommandIntentEngine @Inject constructor() {

    private val intentMatchers: List<IntentMatcher> = listOf(
        // ── Apps ─────────────────────────────────────────────────────────────
        IntentMatcher(
            type = CommandIntentType.OPEN_APP,
            patterns = listOf(
                Regex("""^(?:open|launch|start|run)\s+(?:the\s+)?(.+)$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:go\s+to)\s+(.+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match -> mapOf("app_name" to match.groupValues[1].trim()) }
        ),
        IntentMatcher(
            type = CommandIntentType.CLOSE_APP,
            patterns = listOf(
                Regex("""^(?:close|quit|exit|kill)\s+(?:the\s+)?(.+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match -> mapOf("app_name" to match.groupValues[1].trim()) }
        ),

        // ── Phone / Call ─────────────────────────────────────────────────────
        IntentMatcher(
            type = CommandIntentType.CALL_CONTACT,
            patterns = listOf(
                Regex("""^(?:call|phone|ring)\s+(.+)$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:make\s+a\s+call\s+to)\s+(.+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match -> mapOf("target" to match.groupValues[1].trim()) }
        ),
        IntentMatcher(
            type = CommandIntentType.DIAL_NUMBER,
            patterns = listOf(
                Regex("""^(?:dial)\s+([0-9\+\-\s]+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match -> mapOf("number" to match.groupValues[1].trim()) }
        ),
        IntentMatcher(
            type = CommandIntentType.END_CALL,
            patterns = listOf(
                Regex("""^(?:hang\s*up|end\s+call|disconnect)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // ── SMS ──────────────────────────────────────────────────────────────
        IntentMatcher(
            type = CommandIntentType.SEND_SMS,
            patterns = listOf(
                Regex("""^(?:send\s+(?:a\s+)?(?:sms|message|text)\s+to)\s+([^:]+?)(?:\s+(?:saying|that|message)?\s*[:\s]\s*(.+))?$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:text|message)\s+([^:]+?)(?:\s+(?:that|saying)?\s*[:\s]\s*(.+))?$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match ->
                val recipient = match.groupValues[1].trim()
                val body = if (match.groupValues.size > 2) match.groupValues[2].trim() else ""
                buildMap {
                    put("recipient", recipient)
                    if (body.isNotBlank()) put("body", body)
                }
            }
        ),

        // ── YouTube ──────────────────────────────────────────────────────────
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_PLAY,
            patterns = listOf(
                Regex("""^(?:play)\s+(.+)\s+(?:on\s+youtube)$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:play\s+on\s+youtube)\s+(.+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match -> mapOf("query" to match.groupValues[1].trim()) }
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_SEARCH,
            patterns = listOf(
                Regex("""^(?:search\s+youtube\s+for|search\s+on\s+youtube)\s+(.+)$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:youtube)\s+(.+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match -> mapOf("query" to match.groupValues[1].trim()) }
        ),

        // ── Media Playback ───────────────────────────────────────────────────
        IntentMatcher(
            type = CommandIntentType.MEDIA_PLAY,
            patterns = listOf(
                Regex("""^(?:play|resume|play\s+music|resume\s+music)$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:play)\s+(.+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match ->
                if (match.groupValues.size > 1 && match.groupValues[1].isNotBlank()) {
                    mapOf("track" to match.groupValues[1].trim())
                } else emptyMap()
            }
        ),
        IntentMatcher(
            type = CommandIntentType.MEDIA_PAUSE,
            patterns = listOf(
                Regex("""^(?:pause|pause\s+music|stop\s+music|stop)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.MEDIA_NEXT,
            patterns = listOf(
                Regex("""^(?:next\s+track|next\s+song|skip|next)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.MEDIA_PREV,
            patterns = listOf(
                Regex("""^(?:previous\s+track|previous\s+song|previous|back)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // ── Volume Control ───────────────────────────────────────────────────
        IntentMatcher(
            type = CommandIntentType.VOLUME_UP,
            patterns = listOf(
                Regex("""^(?:volume\s+up|increase\s+volume|turn\s+it\s+up|louder)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.VOLUME_DOWN,
            patterns = listOf(
                Regex("""^(?:volume\s+down|decrease\s+volume|turn\s+it\s+down|quieter|lower\s+volume)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.VOLUME_MUTE,
            patterns = listOf(
                Regex("""^(?:mute|mute\s+volume|silence)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // ── Quick Settings / System ──────────────────────────────────────────
        IntentMatcher(
            type = CommandIntentType.OPEN_SETTINGS,
            patterns = listOf(
                Regex("""^(?:open\s+settings|settings)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.TOGGLE_FLASHLIGHT,
            patterns = listOf(
                Regex("""^(?:turn\s+on\s+flashlight|turn\s+off\s+flashlight|flashlight\s+on|flashlight\s+off|toggle\s+flashlight|flashlight)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match ->
                val text = match.value.lowercase(Locale.ROOT)
                val state = when {
                    text.contains("on") -> "on"
                    text.contains("off") -> "off"
                    else -> "toggle"
                }
                mapOf("state" to state)
            }
        ),
        IntentMatcher(
            type = CommandIntentType.TOGGLE_WIFI,
            patterns = listOf(
                Regex("""^(?:turn\s+on\s+wi-?fi|turn\s+off\s+wi-?fi|wi-?fi\s+on|wi-?fi\s+off|toggle\s+wi-?fi|wi-?fi)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.TOGGLE_BLUETOOTH,
            patterns = listOf(
                Regex("""^(?:turn\s+on\s+bluetooth|turn\s+off\s+bluetooth|bluetooth\s+on|bluetooth\s+off|toggle\s+bluetooth|bluetooth)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // ── Web Search ───────────────────────────────────────────────────────
        IntentMatcher(
            type = CommandIntentType.WEB_SEARCH,
            patterns = listOf(
                Regex("""^(?:search\s+for|search\s+the\s+web\s+for|google|google\s+for|search)\s+(.+)$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:who\s+is|what\s+is|where\s+is|why\s+is|how\s+to)\s+(.+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match ->
                val query = if (match.groupValues.size > 1) match.groupValues[1].trim() else match.value.trim()
                mapOf("query" to query)
            }
        )
    )

    /**
     * Parse raw natural text into a structured [CommandIntent].
     */
    fun parse(rawText: String, source: CommandSource = CommandSource.VOICE): CommandIntent {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return CommandIntent.unknown("", source)
        }

        for (matcher in intentMatchers) {
            for (pattern in matcher.patterns) {
                val match = pattern.find(trimmed)
                if (match != null) {
                    val params = matcher.paramExtractor(match)
                    return CommandIntent(
                        intentType = matcher.type,
                        parameters = params,
                        rawText = trimmed,
                        confidence = 0.95f,
                        requiresConfirmation = matcher.requiresConfirmation,
                        source = source
                    )
                }
            }
        }

        // No pattern matched -> UNKNOWN
        return CommandIntent.unknown(trimmed, source)
    }

    private data class IntentMatcher(
        val type: CommandIntentType,
        val patterns: List<Regex>,
        val requiresConfirmation: Boolean = false,
        val paramExtractor: (MatchResult) -> Map<String, String> = { emptyMap() }
    )
}
