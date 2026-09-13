package com.cypher.assistant.core.intent

import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandSource
import org.junit.Assert.assertEquals
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
    fun parsesYouTubeSearchCommands() {
        val variations = listOf(
            "Cypher search YouTube for Python tutorials",
            "search on YouTube for Kotlin compose",
            "search YouTube for lo fi music",
            "find on YouTube cute cats"
        )
        for (v in variations) {
            val intent = intentEngine.parse(v, CommandSource.VOICE)
            assertEquals("Failed for variation: $v", CommandIntentType.YOUTUBE_SEARCH, intent.intentType)
        }
    }

    @Test
    fun parsesYouTubePlayCommands() {
        val variations = listOf(
            "Cypher play Bohemian Rhapsody on YouTube",
            "play on YouTube classical music",
            "play Naruto opening on YouTube",
            "play on YouTube jazz"
        )
        for (v in variations) {
            val intent = intentEngine.parse(v, CommandSource.VOICE)
            assertEquals("Failed for variation: $v", CommandIntentType.YOUTUBE_PLAY_SEARCH, intent.intentType)
        }
    }

    @Test
    fun parsesYouTubeSectionCommands() {
        assertEquals(CommandIntentType.YOUTUBE_OPEN_SHORTS, intentEngine.parse("open YouTube Shorts", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_SHORTS, intentEngine.parse("shorts", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_SUBSCRIPTIONS, intentEngine.parse("open YouTube subscriptions", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_SUBSCRIPTIONS, intentEngine.parse("my subscriptions", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_HISTORY, intentEngine.parse("open watch history", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_HISTORY, intentEngine.parse("youtube history", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_CHANNEL, intentEngine.parse("open my channel", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_OPEN_HOME, intentEngine.parse("open YouTube home", CommandSource.VOICE).intentType)
    }

    @Test
    fun parsesYouTubeMediaControls() {
        assertEquals(CommandIntentType.YOUTUBE_PAUSE, intentEngine.parse("pause YouTube", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_PAUSE, intentEngine.parse("pause the video", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_RESUME, intentEngine.parse("resume YouTube", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_STOP, intentEngine.parse("stop YouTube", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_NEXT, intentEngine.parse("next video", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_NEXT, intentEngine.parse("skip video", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_PREVIOUS, intentEngine.parse("previous video", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_VOLUME_UP, intentEngine.parse("increase YouTube volume", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_VOLUME_DOWN, intentEngine.parse("decrease YouTube volume", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_MUTE, intentEngine.parse("mute YouTube", CommandSource.VOICE).intentType)
        assertEquals(CommandIntentType.YOUTUBE_UNMUTE, intentEngine.parse("unmute YouTube", CommandSource.VOICE).intentType)
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
    fun parsesPlayOnYouTubeCommand() {
        val intent = intentEngine.parse("play classical music on youtube", CommandSource.VOICE)
        assertEquals(CommandIntentType.YOUTUBE_PLAY_SEARCH, intent.intentType)
        assertEquals("classical music", intent.parameters["query"])
    }

    @Test
    fun parsesVolumeUpCommand() {
        val intent = intentEngine.parse("volume up", CommandSource.VOICE)
        assertEquals(CommandIntentType.VOLUME_UP, intent.intentType)
    }

    @Test
    fun parsesFlashlightToggleCommand() {
        val intent = intentEngine.parse("turn on flashlight", CommandSource.VOICE)
        assertEquals(CommandIntentType.TOGGLE_FLASHLIGHT, intent.intentType)
        assertEquals("on", intent.parameters["state"])
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
