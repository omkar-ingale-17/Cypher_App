package com.cypher.assistant.core.intent

import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommandIntentEngineTest {

    private lateinit var intentEngine: CommandIntentEngine

    @Before
    fun setup() {
        intentEngine = CommandIntentEngine()
    }

    @Test
    fun parsesGreetingHelloCommandCorrectly() {
        val intent = intentEngine.parse("hello", CommandSource.VOICE)
        assertEquals(CommandIntentType.GREETING, intent.intentType)
    }

    @Test
    fun parsesGreetingHeyCypherCommandCorrectly() {
        val intent = intentEngine.parse("hey cypher", CommandSource.VOICE)
        assertEquals(CommandIntentType.GREETING, intent.intentType)
    }

    @Test
    fun parsesOpenYouTubeWithWakeWordVariations() {
        val variations = listOf(
            "Cypher open YouTube",
            "Cypher, open YouTube",
            "open YouTube Cypher",
            "open the YouTube app",
            "Jaan open WhatsApp",
            "Baby open Camera"
        )
        for (v in variations) {
            val intent = intentEngine.parse(v, CommandSource.VOICE)
            assertEquals("Failed for variation: $v", CommandIntentType.OPEN_APP, intent.intentType)
        }
    }

    @Test
    fun parsesMinimizeCommandsAndAsrVariations() {
        val variations = listOf(
            "Cypher minimize",
            "Cypher minimize app",
            "Cypher minimize the app",
            "Cypher go to background",
            "Jaan minimize app",
            "Baby minimize",
            "minimize",
            "minimizer",
            "minimized",
            "hide app"
        )
        for (v in variations) {
            val intent = intentEngine.parse(v, CommandSource.VOICE)
            assertEquals("Failed for variation: $v", CommandIntentType.MINIMIZE_APP, intent.intentType)
        }
    }

    @Test
    fun parsesGoBackCommandsAndAsrVariations() {
        val variations = listOf(
            "Cypher, go back",
            "Cypher go back",
            "Cypher, go back page",
            "Cypher, return back",
            "Cypher go back a page",
            "go back",
            "go back page",
            "return back",
            "go to previous page",
            "previous page",
            "go to previous screen",
            "back"
        )
        for (v in variations) {
            val intent = intentEngine.parse(v, CommandSource.VOICE)
            assertEquals("Failed for variation: $v", CommandIntentType.GO_BACK, intent.intentType)
        }
    }

    @Test
    fun parsesGoHomeCommandsAndAsrVariations() {
        val variations = listOf(
            "Cypher go back to home",
            "Cypher go home",
            "Cypher open home",
            "Cypher show home",
            "Baby go home",
            "Jaan go to home",
            "go home",
            "go to home",
            "home screen",
            "back to home"
        )
        for (v in variations) {
            val intent = intentEngine.parse(v, CommandSource.VOICE)
            assertEquals("Failed for variation: $v", CommandIntentType.GO_HOME, intent.intentType)
        }
    }

    @Test
    fun parsesLockScreenCommandsAndAsrVariations() {
        val variations = listOf(
            "Cypher, screen locked",
            "Cypher, screen lock",
            "Cypher, lock the screen",
            "Cypher lock the screen",
            "Cypher lock screen",
            "Cypher lock my phone",
            "screen locker",
            "screen lock",
            "lock screen",
            "lock my phone"
        )
        for (v in variations) {
            val intent = intentEngine.parse(v, CommandSource.VOICE)
            assertEquals("Failed for variation: $v", CommandIntentType.LOCK_SCREEN, intent.intentType)
        }
    }

    @Test
    fun parsesCloseAppCommands() {
        val variations = listOf(
            "Cypher close Instagram",
            "Cypher close Instagram app",
            "close Instagram",
            "kill Instagram",
            "stop Instagram app"
        )
        for (v in variations) {
            val intent = intentEngine.parse(v, CommandSource.VOICE)
            assertEquals("Failed for variation: $v", CommandIntentType.CLOSE_APP, intent.intentType)
            assertEquals("Failed app_name for: $v", "instagram", intent.parameters["app_name"]?.lowercase())
        }
    }

    // =========================================================================
    // Module 3: YouTube Command & Intent Tests
    // =========================================================================

    @Test
    fun parsesYouTubeSearchCommands() {
        val variations = mapOf(
            "Cypher, search YouTube for Python" to "python",
            "search on YouTube for machine learning" to "machine learning",
            "find Python tutorials on YouTube" to "python tutorials",
            "find machine learning videos" to "machine learning",
            "look for Naruto videos" to "naruto"
        )
        for ((phrase, expectedQuery) in variations) {
            val intent = intentEngine.parse(phrase, CommandSource.VOICE)
            assertEquals("Failed intent for: $phrase", CommandIntentType.YOUTUBE_SEARCH, intent.intentType)
            assertEquals("Failed query for: $phrase", expectedQuery, intent.parameters["query"]?.lowercase())
        }
    }

    @Test
    fun parsesYouTubePlayCommands() {
        val variations = mapOf(
            "Cypher, play Python tutorial" to "python tutorial",
            "play Naruto opening" to "naruto opening",
            "play relaxing music" to "relaxing music",
            "watch Java tutorial on YouTube" to "java tutorial"
        )
        for ((phrase, expectedQuery) in variations) {
            val intent = intentEngine.parse(phrase, CommandSource.VOICE)
            assertEquals("Failed intent for: $phrase", CommandIntentType.YOUTUBE_PLAY_SEARCH, intent.intentType)
            assertEquals("Failed query for: $phrase", expectedQuery, intent.parameters["query"]?.lowercase())
        }
    }

    @Test
    fun parsesYouTubePlayIndexCommands() {
        val variations = mapOf(
            "Cypher, play the first video" to "1",
            "play second video" to "2",
            "select third result" to "3",
            "play 1st video" to "1",
            "open the fourth video" to "4"
        )
        for ((phrase, expectedIdx) in variations) {
            val intent = intentEngine.parse(phrase, CommandSource.VOICE)
            assertEquals("Failed intent for: $phrase", CommandIntentType.YOUTUBE_PLAY_INDEX, intent.intentType)
            assertEquals("Failed index for: $phrase", expectedIdx, intent.parameters["index"])
        }
    }

    @Test
    fun parsesYouTubeLikeAndDislikeCommands() {
        val likePhrases = listOf("Cypher, like this video", "like the video", "thumbs up", "i like this")
        for (phrase in likePhrases) {
            val intent = intentEngine.parse(phrase, CommandSource.VOICE)
            assertEquals("Failed for: $phrase", CommandIntentType.YOUTUBE_LIKE, intent.intentType)
        }

        val dislikePhrases = listOf("Cypher, dislike this video", "dislike video", "thumbs down")
        for (phrase in dislikePhrases) {
            val intent = intentEngine.parse(phrase, CommandSource.VOICE)
            assertEquals("Failed for: $phrase", CommandIntentType.YOUTUBE_DISLIKE, intent.intentType)
        }
    }

    @Test
    fun parsesYouTubeSubscribeAndUnsubscribe() {
        val subPhrases = listOf("Cypher, subscribe", "subscribe to this channel", "subscribe channel")
        for (phrase in subPhrases) {
            val intent = intentEngine.parse(phrase, CommandSource.VOICE)
            assertEquals("Failed for: $phrase", CommandIntentType.YOUTUBE_SUBSCRIBE, intent.intentType)
        }

        val unsubPhrases = listOf("Cypher, unsubscribe", "unsubscribe from this channel")
        for (phrase in unsubPhrases) {
            val intent = intentEngine.parse(phrase, CommandSource.VOICE)
            assertEquals("Failed for: $phrase", CommandIntentType.YOUTUBE_UNSUBSCRIBE, intent.intentType)
            assertTrue("Unsubscribe must require confirmation", intent.requiresConfirmation)
        }
    }

    @Test
    fun parsesYouTubeCommentsAndDescription() {
        assertEquals(CommandIntentType.YOUTUBE_COMMENTS_OPEN, intentEngine.parse("open comments", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_COMMENTS_CLOSE, intentEngine.parse("close comments", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_DESCRIPTION_OPEN, intentEngine.parse("open description", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_SHOW_MORE, intentEngine.parse("show more", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_SHOW_LESS, intentEngine.parse("show less", CommandSource.VOICE).intentType)
    }

    @Test
    fun parsesYouTubeScrollCommands() {
        assertEquals(CommandIntentType.YOUTUBE_SCROLL_DOWN, intentEngine.parse("scroll down", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_SCROLL_DOWN, intentEngine.parse("scroll to comments", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_SCROLL_UP, intentEngine.parse("scroll up", CommandSource.VOICE).intentType)
    }

    @Test
    fun parsesYouTubeNavigationSections() {
        assertEquals(CommandIntentType.YOUTUBE_OPEN_SHORTS, intentEngine.parse("open Shorts", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_SUBSCRIPTIONS, intentEngine.parse("open my subscriptions", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_HISTORY, intentEngine.parse("open YouTube history", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_CHANNEL, intentEngine.parse("open my channel", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_NOTIFICATIONS, intentEngine.parse("open YouTube notifications", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_HOME, intentEngine.parse("go to YouTube home", CommandSource.VOICE).intentType)
    }

    @Test
    fun parsesYouTubePlaybackAndVolumeCommands() {
        assertEquals(CommandIntentType.YOUTUBE_PAUSE, intentEngine.parse("pause", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_RESUME, intentEngine.parse("resume", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_STOP, intentEngine.parse("stop video", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_NEXT, intentEngine.parse("next video", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_PREVIOUS, intentEngine.parse("previous video", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_VOLUME_UP, intentEngine.parse("increase volume", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_VOLUME_DOWN, intentEngine.parse("decrease volume", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_MUTE, intentEngine.parse("mute", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_UNMUTE, intentEngine.parse("unmute", CommandSource.VOICE).intentType)
    }

    @Test
    fun normalizesAsrVariationsForYouTube() {
        val query1 = intentEngine.parse("search you tube for Kotlin", CommandSource.VOICE)
        assertEquals(CommandIntentType.YOUTUBE_SEARCH, query1.intentType)
        assertEquals("kotlin", query1.parameters["query"]?.lowercase())

        val query2 = intentEngine.parse("search u tube for Kotlin", CommandSource.VOICE)
        assertEquals(CommandIntentType.YOUTUBE_SEARCH, query2.intentType)
        assertEquals("kotlin", query2.parameters["query"]?.lowercase())

        val query3 = intentEngine.parse("open you-tube", CommandSource.VOICE)
        assertEquals(CommandIntentType.OPEN_APP, query3.intentType)
    }

    // =========================================================================
    // General Assistant Tests
    // =========================================================================

    @Test
    fun parsesGetTimeCommands() {
        val variations = listOf(
            "what is the time",
            "what's the time",
            "what time is it",
            "current time",
            "tell me the time",
            "time please"
        )
        for (v in variations) {
            val intent = intentEngine.parse(v, CommandSource.VOICE)
            assertEquals("Failed for variation: $v", CommandIntentType.GET_TIME, intent.intentType)
        }
    }

    @Test
    fun parsesGetDateCommands() {
        val variations = listOf(
            "what is the date",
            "what's the date",
            "what's today's date",
            "what day is today",
            "tell me the date"
        )
        for (v in variations) {
            val intent = intentEngine.parse(v, CommandSource.VOICE)
            assertEquals("Failed for variation: $v", CommandIntentType.GET_DATE, intent.intentType)
        }
    }

    @Test
    fun parsesThankYouCommand() {
        val intent = intentEngine.parse("thank you", CommandSource.VOICE)
        assertEquals(CommandIntentType.THANK_YOU, intent.intentType)
    }

    @Test
    fun parsesGoodbyeCommand() {
        val intent = intentEngine.parse("goodbye", CommandSource.VOICE)
        assertEquals(CommandIntentType.GOODBYE, intent.intentType)
    }

    @Test
    fun parsesCallContactCommandCorrectly() {
        val intent = intentEngine.parse("call John Doe", CommandSource.VOICE)
        assertEquals(CommandIntentType.CALL_CONTACT, intent.intentType)
        assertEquals("John Doe", intent.parameters["target"])
    }

    @Test
    fun parsesDialNumberCommandCorrectly() {
        val intent = intentEngine.parse("dial +123456789", CommandSource.VOICE)
        assertEquals(CommandIntentType.DIAL_NUMBER, intent.intentType)
        assertEquals("+123456789", intent.parameters["number"])
    }

    @Test
    fun parsesVolumeUpCommand() {
        val intent = intentEngine.parse("volume up", CommandSource.VOICE)
        assertEquals(CommandIntentType.YOUTUBE_VOLUME_UP, intent.intentType)
    }

    @Test
    fun parsesFlashlightToggleCommand() {
        val intent = intentEngine.parse("turn on flashlight", CommandSource.VOICE)
        assertEquals(CommandIntentType.TOGGLE_FLASHLIGHT, intent.intentType)
    }

    @Test
    fun parsesWebSearchQuery() {
        val intent = intentEngine.parse("search for distance to the moon", CommandSource.VOICE)
        assertEquals(CommandIntentType.WEB_SEARCH, intent.intentType)
        assertEquals("distance to the moon", intent.parameters["query"])
    }

    @Test
    fun returnsUnknownForUnsupportedFreeFormText() {
        val intent = intentEngine.parse("some completely unrecognized sentence 12345", CommandSource.VOICE)
        assertEquals(CommandIntentType.UNKNOWN, intent.intentType)
    }
}
