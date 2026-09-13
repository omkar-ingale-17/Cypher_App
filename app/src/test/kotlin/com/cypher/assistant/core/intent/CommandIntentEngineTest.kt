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
        assertEquals(CommandIntentType.YOUTUBE_PLAY, intent.intentType)
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
