package com.cypher.assistant.core.intent

import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandSource
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic regular expression and keyword-based NLU Engine with robust ASR normalization.
 *
 * Pattern priority is critical:
 * 1. Conversational greetings and persona intents
 * 2. Time, Date, and status queries
 * 3. System Navigation: MINIMIZE_APP / GO_HOME, LOCK_SCREEN
 * 4. YouTube Control: YOUTUBE_* (Must precede OPEN_APP to avoid "open youtube shorts" being treated as generic app launch)
 * 5. App Control: OPEN_APP, CLOSE_APP, LIST_APPS, OPEN_SETTINGS
 * 6. Specific device verbs (call, dial, sms, media, volume, settings)
 * 7. General fallbacks (web search, unknown)
 */
@Singleton
class CommandIntentEngine @Inject constructor() {

    private val intentMatchers: List<IntentMatcher> = listOf(
        // -- Conversational & Persona ------------------------------------------
        IntentMatcher(
            type = CommandIntentType.GREETING,
            patterns = listOf(
                Regex("""^(?:hello|hi|hey|greetings|good\s+morning|good\s+afternoon|good\s+evening|namaste|howdy)(?:\s+cypher)?$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.ASSISTANT_NAME,
            patterns = listOf(
                Regex("""^(?:what\s+is\s+your\s+name|what'?s\s+your\s+name|who\s+are\s+you|tell\s+me\s+your\s+name)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.ASSISTANT_STATUS,
            patterns = listOf(
                Regex("""^(?:how\s+are\s+you|how\s+are\s+you\s+doing|how'?s\s+it\s+going|how\s+is\s+it\s+going|how\s+do\s+you\s+do|are\s+you\s+okay|how\s+is\s+everything)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.ASSISTANT_CAPABILITIES,
            patterns = listOf(
                Regex("""^(?:what\s+can\s+you\s+do|help|what\s+are\s+your\s+features|what\s+do\s+you\s+do|show\s+help|capabilities)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.SET_USER_NAME,
            patterns = listOf(
                Regex("""^(?:my\s+name\s+is|call\s+me|i\s+am)\s+([a-zA-Z\s]+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match -> mapOf("name" to match.groupValues[1].trim()) }
        ),
        IntentMatcher(
            type = CommandIntentType.GET_USER_NAME,
            patterns = listOf(
                Regex("""^(?:what\s+is\s+my\s+name|what'?s\s+my\s+name|who\s+am\s+i|do\s+you\s+know\s+my\s+name)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // -- Time & Date ------------------------------------------------------
        IntentMatcher(
            type = CommandIntentType.GET_TIME,
            patterns = listOf(
                Regex("""^(?:what\s+is\s+the\s+time|what\s+time\s+is\s+it|what'?s\s+the\s+time|tell\s+me\s+the\s+time|current\s+time|time\s+please|can\s+you\s+tell\s+me\s+the\s+time|the\s+time|time)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.GET_DATE,
            patterns = listOf(
                Regex("""^(?:what[\s']*s?\s+(?:today[\s']*s?\s+)?date|what\s+is\s+(?:today[\s']*s?\s+)?(?:the\s+)?date|what[\s']*s?\s+the\s+date|what\s+day\s+is\s+(?:today|it)|today[\s']*s?\s+date|date\s+please|tell\s+me\s+the\s+date|date)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.THANK_YOU,
            patterns = listOf(
                Regex("""^(?:thank\s+you|thanks|thank\s+you\s+so\s+much|thanks\s+a\s+lot|thanks\s+cypher)(?:\s+cypher)?$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.GOODBYE,
            patterns = listOf(
                Regex("""^(?:goodbye|bye|bye\s+bye|see\s+you|see\s+you\s+later|exit|quit|sleep)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // -- System Navigation & Device Control -------------------------------
        IntentMatcher(
            type = CommandIntentType.MINIMIZE_APP,
            patterns = listOf(
                Regex("""^(?:minimize|minimise)(?:\s+(?:the\s+)?(?:app|screen|cypher))?$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:go\s+to\s+background|send\s+to\s+background|background\s+app)$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:hide|hide\s+(?:the\s+)?app|hide\s+cypher)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.GO_HOME,
            patterns = listOf(
                Regex("""^(?:go\s+(?:back\s+)?(?:to\s+)?home(?:\s+screen)?|open\s+home(?:\s+screen)?|show\s+home(?:\s+screen)?|home\s+screen|back\s+to\s+home|go\s+home|home)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.LOCK_SCREEN,
            patterns = listOf(
                Regex("""^(?:lock(?:\s+the)?\s+screen|screen\s+lock(?:ed|er)?|lock\s+(?:the\s+|my\s+)?phone|lock\s+(?:the\s+|my\s+)?device|turn\s+off\s+screen|lock)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // -- YouTube Control (Module 3) - Precedes general app matchers --------
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_HOME,
            patterns = listOf(
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:open|go\s+to|show|launch)\s+youtube\s+home(?:\s+(?:for\s+me|please))?$""", RegexOption.IGNORE_CASE),
                Regex("""^youtube\s+home$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_SHORTS,
            patterns = listOf(
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:open|show|go\s+to|launch)\s+(?:the\s+|my\s+)?(?:youtube\s+)?shorts(?:\s+(?:for\s+me|please))?$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:youtube\s+)?shorts$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_SUBSCRIPTIONS,
            patterns = listOf(
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:open|show|go\s+to|launch)\s+(?:the\s+|my\s+)?(?:youtube\s+)?(?:subscriptions|subs)(?:\s+(?:for\s+me|please))?$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:my\s+)?(?:youtube\s+)?(?:subscriptions|subs)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_HISTORY,
            patterns = listOf(
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:open|show|go\s+to|launch)\s+(?:the\s+|my\s+)?(?:youtube\s+)?(?:watch\s+)?history(?:\s+(?:for\s+me|please))?$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:my\s+)?(?:youtube\s+)?(?:watch\s+)?history$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_CHANNEL,
            patterns = listOf(
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:open|show|go\s+to|launch)\s+(?:the\s+|my\s+)?(?:youtube\s+)?channel(?:\s+(?:for\s+me|please))?$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:my\s+)?(?:youtube\s+)?channel$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_PAUSE,
            patterns = listOf(
                Regex("""^(?:pause\s+youtube|pause\s+(?:the\s+)?video|pause\s+playback)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_RESUME,
            patterns = listOf(
                Regex("""^(?:resume\s+youtube|unpause\s+youtube|resume\s+(?:the\s+)?video|continue\s+(?:the\s+)?video|continue\s+playing)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_STOP,
            patterns = listOf(
                Regex("""^(?:stop\s+youtube|stop\s+(?:the\s+)?video|stop\s+playback)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_NEXT,
            patterns = listOf(
                Regex("""^(?:next\s+video|next\s+youtube\s+video|skip\s+video|skip\s+to\s+next\s+video|play\s+next\s+video)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_PREVIOUS,
            patterns = listOf(
                Regex("""^(?:previous\s+video|previous\s+youtube\s+video|play\s+previous\s+video|go\s+back\s+video|last\s+video)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_VOLUME_UP,
            patterns = listOf(
                Regex("""^(?:increase\s+youtube\s+volume|turn\s+up\s+youtube|volume\s+up\s+youtube|youtube\s+volume\s+up|louder\s+youtube)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_VOLUME_DOWN,
            patterns = listOf(
                Regex("""^(?:decrease\s+youtube\s+volume|turn\s+down\s+youtube|volume\s+down\s+youtube|youtube\s+volume\s+down|lower\s+youtube\s+volume|quieter\s+youtube)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_MUTE,
            patterns = listOf(
                Regex("""^(?:mute\s+youtube|youtube\s+mute|silence\s+youtube)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_UNMUTE,
            patterns = listOf(
                Regex("""^(?:unmute\s+youtube|youtube\s+unmute)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_PLAY_SEARCH,
            patterns = listOf(
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:play|start)\s+(.+?)\s+(?:on\s+youtube|in\s+youtube|from\s+youtube)(?:\s+(?:for\s+me|please))?$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:play|start)\s+(?:on\s+youtube|in\s+youtube|from\s+youtube|youtube)\s+(.+?)(?:\s+(?:for\s+me|please))?$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match -> mapOf("query" to match.groupValues[1].trim()) }
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_SEARCH,
            patterns = listOf(
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:search|find|look\s+up)\s+(?:for\s+)?(.+?)\s+(?:on\s+youtube|in\s+youtube)(?:\s+(?:for\s+me|please))?$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:search\s+youtube\s+for|search\s+on\s+youtube\s+for|search\s+on\s+youtube|search\s+youtube|find\s+on\s+youtube|youtube\s+search)\s+(.+?)(?:\s+(?:for\s+me|please))?$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match -> mapOf("query" to match.groupValues[1].trim()) }
        ),

        // -- System Settings & Navigation -------------------------------------
        IntentMatcher(
            type = CommandIntentType.OPEN_SETTINGS,
            patterns = listOf(
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:open|launch|go\s+to)\s+(?:the\s+)?(?:system\s+)?settings$""", RegexOption.IGNORE_CASE),
                Regex("""^settings$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { mapOf("setting_type" to "settings") }
        ),

        // -- Apps Control (Module 2) ------------------------------------------
        IntentMatcher(
            type = CommandIntentType.LIST_APPS,
            patterns = listOf(
                Regex("""^(?:list\s+(?:all\s+)?apps|list\s+my\s+apps|show\s+(?:all\s+)?apps|what\s+apps\s+are\s+installed|show\s+my\s+apps|installed\s+apps)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.OPEN_APP,
            patterns = listOf(
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:open|launch|start|run|go\s+to)\s+(?:the\s+|my\s+)?(.+?)(?:\s+(?:app|for\s+me|please|cypher|cipher|jaan|jan|baby))?$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:search\s+my\s+apps\s+for|search\s+apps\s+for|find\s+app|find)\s+(.+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match ->
                val rawName = match.groupValues[1].trim()
                val cleanName = rawName.replace(Regex("""\s+(?:app|for\s+me|please|cypher|cipher|jaan|jan|baby)$""", RegexOption.IGNORE_CASE), "").trim()
                mapOf("app_name" to cleanName)
            }
        ),
        IntentMatcher(
            type = CommandIntentType.CLOSE_APP,
            patterns = listOf(
                Regex("""^(?:can\s+you\s+|could\s+you\s+|please\s+)?(?:close|kill|stop|force\s+stop)\s+(?:the\s+|my\s+)?(.+?)(?:\s+(?:app|for\s+me|please|cypher|cipher|jaan|jan|baby))?$""", RegexOption.IGNORE_CASE),
                Regex("""^close\s+(?:the\s+)?app$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match ->
                val rawName = if (match.groupValues.size > 1) match.groupValues[1].trim() else ""
                val cleanName = rawName.replace(Regex("""\s+(?:app|for\s+me|please|cypher|cipher|jaan|jan|baby)$""", RegexOption.IGNORE_CASE), "").trim()
                mapOf("app_name" to cleanName)
            }
        ),

        // -- Phone / Call -----------------------------------------------------
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

        // -- SMS --------------------------------------------------------------
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

        // -- Generic Media Playback -------------------------------------------
        IntentMatcher(
            type = CommandIntentType.MEDIA_PLAY,
            patterns = listOf(
                Regex("""^(?:play|resume|play\s+music|resume\s+music)$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:play\s+song|start\s+music)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.MEDIA_PAUSE,
            patterns = listOf(
                Regex("""^(?:pause|pause\s+music|pause\s+song|stop\s+music)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.MEDIA_NEXT,
            patterns = listOf(
                Regex("""^(?:next\s+track|next\s+song|next|skip)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.MEDIA_PREV,
            patterns = listOf(
                Regex("""^(?:previous\s+track|previous\s+song|previous|back)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // -- Generic Volume Control -------------------------------------------
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

        // -- Quick Settings / Toggles -----------------------------------------
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

        // -- Web Search -------------------------------------------------------
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
     * Pre-processes raw natural speech text into normalized form:
     * - Expands contractions ("what's" -> "what is")
     * - Strips punctuation and excessive whitespace
     * - Strips leading and trailing wake words and conversational filler words
     * - Normalizes common ASR phonetic variations
     */
    fun preProcessText(rawText: String): String {
        var text = rawText.trim().lowercase(Locale.ROOT)
        if (text.isBlank()) return ""

        // 1. Expand standard conversational contractions with word boundary
        text = text.replace(Regex("""\bwhat's\b""", RegexOption.IGNORE_CASE), "what is")
            .replace(Regex("""\bwhats\b""", RegexOption.IGNORE_CASE), "what is")
            .replace(Regex("""\bwho's\b""", RegexOption.IGNORE_CASE), "who is")
            .replace(Regex("""\bhow's\b""", RegexOption.IGNORE_CASE), "how is")
            .replace(Regex("""\btoday's\b""", RegexOption.IGNORE_CASE), "today")

        // 2. Remove punctuation marks
        text = text.replace(Regex("""[,.?!:;\-_"']+"""), " ")
        text = text.replace(Regex("""\s+"""), " ").trim()

        // 3. Strip leading / trailing wake words and prefixes
        val prefixes = listOf("hey", "ok", "okay", "hello", "hi", "yo", "dear")
        val wakeWords = listOf("cypher", "cipher", "jaan", "jann", "jan", "baby")

        var changed = true
        while (changed) {
            changed = false
            for (w in wakeWords) {
                for (p in prefixes) {
                    val pat = Regex("""^$p\s+$w\s+""", RegexOption.IGNORE_CASE)
                    if (pat.containsMatchIn(text)) {
                        text = pat.replace(text, "").trim()
                        changed = true
                    }
                }
                val leadPat = Regex("""^$w\s+""", RegexOption.IGNORE_CASE)
                if (leadPat.containsMatchIn(text)) {
                    text = leadPat.replace(text, "").trim()
                    changed = true
                }
                val trailPat = Regex("""\s+$w$""", RegexOption.IGNORE_CASE)
                if (trailPat.containsMatchIn(text)) {
                    text = trailPat.replace(text, "").trim()
                    changed = true
                }
            }
        }

        // 4. Common ASR phonetic replacements
        val replacements = listOf(
            Regex("""\bminimizer\b""", RegexOption.IGNORE_CASE) to "minimize",
            Regex("""\bminimiser\b""", RegexOption.IGNORE_CASE) to "minimize",
            Regex("""\bminimized\b""", RegexOption.IGNORE_CASE) to "minimize",
            Regex("""\bminimised\b""", RegexOption.IGNORE_CASE) to "minimize",
            Regex("""\bscreen locked\b""", RegexOption.IGNORE_CASE) to "lock screen",
            Regex("""\bscreen locker\b""", RegexOption.IGNORE_CASE) to "lock screen",
            Regex("""\bscreen lock\b""", RegexOption.IGNORE_CASE) to "lock screen",
            Regex("""\byou tube\b""", RegexOption.IGNORE_CASE) to "youtube",
            Regex("""\byou-tube\b""", RegexOption.IGNORE_CASE) to "youtube",
            Regex("""\bwhat\s+s\s+app\b""", RegexOption.IGNORE_CASE) to "whatsapp",
            Regex("""\bwhats\s+app\b""", RegexOption.IGNORE_CASE) to "whatsapp",
            Regex("""\bface\s+book\b""", RegexOption.IGNORE_CASE) to "facebook",
            Regex("""\binsta\s+gram\b""", RegexOption.IGNORE_CASE) to "instagram",
            Regex("""\bg\s+mail\b""", RegexOption.IGNORE_CASE) to "gmail",
            Regex("""\bplay\s+store\b""", RegexOption.IGNORE_CASE) to "play store",
            Regex("""\bplaystore\b""", RegexOption.IGNORE_CASE) to "play store"
        )
        for ((pattern, repl) in replacements) {
            text = pattern.replace(text, repl)
        }

        return text.replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Parse raw natural text into a structured [CommandIntent].
     */
    fun parse(rawText: String, source: CommandSource = CommandSource.VOICE): CommandIntent {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return CommandIntent.unknown("", source)
        }

        val normalized = preProcessText(trimmed)
        if (normalized.isBlank()) {
            return CommandIntent.unknown(trimmed, source)
        }

        for (matcher in intentMatchers) {
            for (pattern in matcher.patterns) {
                val normMatch = pattern.find(normalized)
                if (normMatch != null) {
                    val trimmedMatch = pattern.find(trimmed)
                    val chosenMatch = if (trimmedMatch != null &&
                        !trimmedMatch.groupValues.any { it.contains("cypher", ignoreCase = true) || it.contains("baby", ignoreCase = true) || it.contains("jaan", ignoreCase = true) }
                    ) {
                        trimmedMatch
                    } else {
                        normMatch
                    }
                    val params = matcher.paramExtractor(chosenMatch)
                    return CommandIntent(
                        intentType = matcher.type,
                        parameters = params,
                        rawText = trimmed,
                        confidence = 0.95f,
                        requiresConfirmation = matcher.requiresConfirmation,
                        source = source
                    )
                }

                val directMatch = pattern.find(trimmed)
                if (directMatch != null) {
                    val params = matcher.paramExtractor(directMatch)
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
