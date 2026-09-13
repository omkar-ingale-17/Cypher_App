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
 * Pattern priority:
 * 1. Conversational greetings and persona intents
 * 2. Time, Date, and status queries
 * 3. System Navigation: MINIMIZE_APP, GO_HOME, GO_BACK, LOCK_SCREEN
 * 4. Specific YouTube Control (Module 3) before generic app launcher / web search
 * 5. App Control (Module 2): OPEN_APP, CLOSE_APP, LIST_APPS, OPEN_SETTINGS
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
            type = CommandIntentType.GO_BACK,
            patterns = listOf(
                Regex("""^(?:go\s+back(?:\s+(?:a\s+)?page)?|return\s+back|go\s+to\s+previous\s+page|previous\s+page|go\s+to\s+previous\s+screen|previous\s+screen|back)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.LOCK_SCREEN,
            patterns = listOf(
                Regex("""^(?:lock(?:\s+the)?\s+screen|screen\s+lock(?:ed|er)?|lock\s+(?:the\s+|my\s+)?phone|lock\s+(?:the\s+|my\s+)?device|turn\s+off(?:\s+the)?\s+screen|lock)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // ======================================================================
        // -- Module 3: YouTube App Control Intents -----------------------------
        // ======================================================================

        // Specific video index play: "play the first video", "play second video"
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_PLAY_INDEX,
            patterns = listOf(
                Regex("""^(?:play|open|select)(?:\s+the)?\s+(first|1st|second|2nd|third|3rd|fourth|4th|fifth|5th)\s+(?:video|result|one)$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:first|second|third|fourth|fifth)\s+video$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match ->
                val ord = match.groupValues[1].lowercase()
                val idx = when (ord) {
                    "first", "1st" -> 1
                    "second", "2nd" -> 2
                    "third", "3rd" -> 3
                    "fourth", "4th" -> 4
                    "fifth", "5th" -> 5
                    else -> 1
                }
                mapOf("index" to idx.toString())
            }
        ),

        // YouTube Search: "search youtube for python", "search for python on youtube", "find python videos"
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_SEARCH,
            patterns = listOf(
                Regex("""^(?:search\s+youtube\s+for|search\s+on\s+youtube\s+for|search\s+youtube|youtube\s+search(?:\s+for)?)\s+(.+)$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:search|find|look\s+for)\s+(.+?)\s+(?:on|in)\s+youtube$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:find|look\s+for)\s+(.+?)\s+videos$""", RegexOption.IGNORE_CASE),
                Regex("""^search\s+youtube$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match ->
                val query = if (match.groupValues.size > 1) match.groupValues[1].trim() else ""
                mapOf("query" to query)
            }
        ),

        // YouTube Play Search: "play python tutorial", "play naruto opening", "play relaxing music on youtube"
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_PLAY_SEARCH,
            patterns = listOf(
                Regex("""^(?:play|watch)\s+(.+?)(?:\s+(?:on|in)\s+youtube)?$""", RegexOption.IGNORE_CASE),
                Regex("""^(?:play\s+video|watch\s+video)\s+(.+)$""", RegexOption.IGNORE_CASE)
            ),
            paramExtractor = { match ->
                val query = match.groupValues[1].trim()
                mapOf("query" to query)
            }
        ),

        // Like / Dislike
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_LIKE,
            patterns = listOf(
                Regex("""^(?:like\s+this\s+video|like\s+video|like\s+the\s+video|like\s+it|i\s+like\s+this|thumbs\s+up)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_DISLIKE,
            patterns = listOf(
                Regex("""^(?:dislike\s+this\s+video|dislike\s+video|dislike\s+the\s+video|dislike\s+it|thumbs\s+down)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // Subscribe / Unsubscribe (Unsubscribe requires confirmation)
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_SUBSCRIBE,
            patterns = listOf(
                Regex("""^(?:subscribe\s+to\s+this\s+channel|subscribe\s+to\s+channel|subscribe\s+channel|subscribe)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_UNSUBSCRIBE,
            patterns = listOf(
                Regex("""^(?:unsubscribe\s+from\s+this\s+channel|unsubscribe\s+from\s+channel|unsubscribe\s+channel|unsubscribe)$""", RegexOption.IGNORE_CASE)
            ),
            requiresConfirmation = true
        ),

        // Comments
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_COMMENTS_OPEN,
            patterns = listOf(
                Regex("""^(?:open\s+comments|show\s+comments|view\s+comments|read\s+comments)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_COMMENTS_CLOSE,
            patterns = listOf(
                Regex("""^(?:close\s+comments|hide\s+comments|dismiss\s+comments)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // Description / Show More / Show Less
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_DESCRIPTION_OPEN,
            patterns = listOf(
                Regex("""^(?:open\s+description|show\s+description|view\s+description|expand\s+description)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_SHOW_MORE,
            patterns = listOf(
                Regex("""^(?:show\s+more|read\s+more|more\s+details|expand)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_SHOW_LESS,
            patterns = listOf(
                Regex("""^(?:show\s+less|read\s+less|collapse)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // Scrolling
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_SCROLL_DOWN,
            patterns = listOf(
                Regex("""^(?:scroll\s+down(?:\s+a\s+little)?|scroll\s+to\s+comments|page\s+down)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_SCROLL_UP,
            patterns = listOf(
                Regex("""^(?:scroll\s+up(?:\s+a\s+little)?|page\s+up)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // YouTube Navigation sections
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_SHORTS,
            patterns = listOf(
                Regex("""^(?:open\s+youtube\s+shorts|open\s+shorts|shorts|show\s+shorts|go\s+to\s+shorts)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_SUBSCRIPTIONS,
            patterns = listOf(
                Regex("""^(?:open\s+my\s+subscriptions|open\s+subscriptions|my\s+subscriptions|show\s+subscriptions|go\s+to\s+subscriptions)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_HISTORY,
            patterns = listOf(
                Regex("""^(?:open\s+youtube\s+history|open\s+my\s+history|open\s+history|show\s+history|go\s+to\s+history)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_CHANNEL,
            patterns = listOf(
                Regex("""^(?:open\s+my\s+channel|open\s+channel|my\s+channel|show\s+my\s+channel|go\s+to\s+my\s+channel)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_NOTIFICATIONS,
            patterns = listOf(
                Regex("""^(?:open\s+youtube\s+notifications|open\s+notifications|show\s+notifications|go\s+to\s+notifications)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_OPEN_HOME,
            patterns = listOf(
                Regex("""^(?:go\s+to\s+youtube\s+home|open\s+youtube\s+home|youtube\s+home)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // YouTube Media Controls
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_PAUSE,
            patterns = listOf(
                Regex("""^(?:pause\s+youtube|pause\s+the\s+video|pause\s+video|pause)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_RESUME,
            patterns = listOf(
                Regex("""^(?:resume\s+youtube|resume\s+the\s+video|resume\s+video|resume|continue\s+video|continue)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_STOP,
            patterns = listOf(
                Regex("""^(?:stop\s+youtube|stop\s+video|stop\s+playback|stop)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_NEXT,
            patterns = listOf(
                Regex("""^(?:next\s+video|skip\s+this\s+video|skip\s+video|play\s+next(?:\s+video)?|next)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_PREVIOUS,
            patterns = listOf(
                Regex("""^(?:previous\s+video|play\s+previous(?:\s+video)?|play\s+last\s+video|previous)$""", RegexOption.IGNORE_CASE)
            )
        ),

        // YouTube / System Volume
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_VOLUME_UP,
            patterns = listOf(
                Regex("""^(?:increase\s+volume|volume\s+up|turn\s+up\s+the\s+volume|louder)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_VOLUME_DOWN,
            patterns = listOf(
                Regex("""^(?:decrease\s+volume|volume\s+down|turn\s+down\s+the\s+volume|lower\s+volume|quieter)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_MUTE,
            patterns = listOf(
                Regex("""^(?:mute\s+video|mute\s+youtube|mute)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.YOUTUBE_UNMUTE,
            patterns = listOf(
                Regex("""^(?:unmute\s+video|unmute\s+youtube|unmute)$""", RegexOption.IGNORE_CASE)
            )
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

        // -- Quick Settings / Toggles -----------------------------------------
        IntentMatcher(
            type = CommandIntentType.TOGGLE_FLASHLIGHT,
            patterns = listOf(
                Regex("""^(?:turn\s+on\s+flashlight|turn\s+off\s+flashlight|flashlight\s+on|flashlight\s+off|toggle\s+flashlight|torch\s+on|torch\s+off|flashlight|torch)$""", RegexOption.IGNORE_CASE)
            )
        ),
        IntentMatcher(
            type = CommandIntentType.TOGGLE_WIFI,
            patterns = listOf(
                Regex("""^(?:turn\s+on\s+wifi|turn\s+off\s+wifi|wifi\s+on|wifi\s+off|toggle\s+wifi|wifi)$""", RegexOption.IGNORE_CASE)
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
            Regex("""\bu tube\b""", RegexOption.IGNORE_CASE) to "youtube",
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
