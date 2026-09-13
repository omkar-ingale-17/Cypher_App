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
    fun parsesWhatIsYourNameCommandCorrectly() {
        val intent = intentEngine.parse("what is your name", CommandSource.VOICE)
        assertEquals(CommandIntentType.ASSISTANT_NAME, intent.intentType)
    }

    @Test
    fun parsesWhoAreYouCommandCorrectly() {
        val intent = intentEngine.parse("who are you", CommandSource.VOICE)
        assertEquals(CommandIntentType.ASSISTANT_NAME, intent.intentType)
    }

    @Test
    fun parsesHowAreYouCommandCorrectly() {
        val intent = intentEngine.parse("how are you", CommandSource.VOICE)
        assertEquals(CommandIntentType.ASSISTANT_STATUS, intent.intentType)
    }

    @Test
    fun parsesWhatCanYouDoCommandCorrectly() {
        val intent = intentEngine.parse("what can you do", CommandSource.VOICE)
        assertEquals(CommandIntentType.ASSISTANT_CAPABILITIES, intent.intentType)
    }

    @Test
    fun parsesMyNameIsCommandAndExtractsName() {
        val intent = intentEngine.parse("my name is Bruce", CommandSource.VOICE)
        assertEquals(CommandIntentType.SET_USER_NAME, intent.intentType)
        assertEquals("Bruce", intent.parameters["name"])
    }

    @Test
    fun parsesWhatIsMyNameCommand() {
        val intent = intentEngine.parse("what is my name", CommandSource.VOICE)
        assertEquals(CommandIntentType.GET_USER_NAME, intent.intentType)
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
    fun parsesOpenWhatsAppCommandCorrectly() {
        val intent = intentEngine.parse("open WhatsApp", CommandSource.VOICE)
        assertEquals(CommandIntentType.OPEN_APP, intent.intentType)
        assertEquals("WhatsApp", intent.parameters["app_name"])
    }

    @Test
    fun parsesLaunchCameraCommandCorrectly() {
        val intent = intentEngine.parse("launch Camera", CommandSource.TEXT)
        assertEquals(CommandIntentType.OPEN_APP, intent.intentType)
        assertEquals("Camera", intent.parameters["app_name"])
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
