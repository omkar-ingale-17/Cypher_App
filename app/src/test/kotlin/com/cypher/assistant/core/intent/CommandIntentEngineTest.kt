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
    fun `parses open whatsapp command correctly`() {
        val intent = intentEngine.parse("open WhatsApp", CommandSource.VOICE)
        assertEquals(CommandIntentType.OPEN_APP, intent.intentType)
        assertEquals("WhatsApp", intent.parameters["app_name"])
    }

    @Test
    fun `parses launch camera command correctly`() {
        val intent = intentEngine.parse("launch Camera", CommandSource.TEXT)
        assertEquals(CommandIntentType.OPEN_APP, intent.intentType)
        assertEquals("Camera", intent.parameters["app_name"])
    }

    @Test
    fun `parses call contact command correctly`() {
        val intent = intentEngine.parse("call John Doe", CommandSource.VOICE)
        assertEquals(CommandIntentType.CALL_CONTACT, intent.intentType)
        assertEquals("John Doe", intent.parameters["target"])
    }

    @Test
    fun `parses dial number command correctly`() {
        val intent = intentEngine.parse("dial +123456789", CommandSource.VOICE)
        assertEquals(CommandIntentType.DIAL_NUMBER, intent.intentType)
        assertEquals("+123456789", intent.parameters["number"])
    }

    @Test
    fun `parses send sms command with recipient and message`() {
        val intent = intentEngine.parse("send message to Alice saying I will be late", CommandSource.VOICE)
        assertEquals(CommandIntentType.SEND_SMS, intent.intentType)
        assertEquals("Alice", intent.parameters["recipient"])
        assertEquals("I will be late", intent.parameters["body"])
    }

    @Test
    fun `parses play on youtube command`() {
        val intent = intentEngine.parse("play classical music on YouTube", CommandSource.VOICE)
        assertEquals(CommandIntentType.YOUTUBE_PLAY, intent.intentType)
        assertEquals("classical music", intent.parameters["query"])
    }

    @Test
    fun `parses volume up command`() {
        val intent = intentEngine.parse("volume up", CommandSource.VOICE)
        assertEquals(CommandIntentType.VOLUME_UP, intent.intentType)
    }

    @Test
    fun `parses flashlight toggle command`() {
        val intent = intentEngine.parse("turn on flashlight", CommandSource.VOICE)
        assertEquals(CommandIntentType.TOGGLE_FLASHLIGHT, intent.intentType)
        assertEquals("on", intent.parameters["state"])
    }

    @Test
    fun `parses web search query`() {
        val intent = intentEngine.parse("search for distance to the moon", CommandSource.VOICE)
        assertEquals(CommandIntentType.WEB_SEARCH, intent.intentType)
        assertEquals("distance to the moon", intent.parameters["query"])
    }

    @Test
    fun `returns UNKNOWN for unsupported free-form text`() {
        val intent = intentEngine.parse("some completely unrecognized sentence", CommandSource.VOICE)
        assertEquals(CommandIntentType.UNKNOWN, intent.intentType)
    }
}
