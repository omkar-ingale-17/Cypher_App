package com.cypher.assistant.core.command

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRouterTest {

    private class TestAppHandler : CommandHandler {
        override val supportedIntents: Set<CommandIntentType> = setOf(
            CommandIntentType.OPEN_APP,
            CommandIntentType.CLOSE_APP
        )

        override suspend fun handle(intent: CommandIntent): CommandResult {
            return when (intent.intentType) {
                CommandIntentType.OPEN_APP -> CommandResult.success("Opening ${intent.parameters["app_name"]}")
                CommandIntentType.CLOSE_APP -> CommandResult.success("Closing ${intent.parameters["app_name"]}")
                else -> CommandResult.failure("Unsupported")
            }
        }
    }

    private class TestMediaHandler : CommandHandler {
        override val supportedIntents: Set<CommandIntentType> = setOf(
            CommandIntentType.MEDIA_PLAY,
            CommandIntentType.MEDIA_PAUSE
        )

        override suspend fun handle(intent: CommandIntent): CommandResult {
            return CommandResult.success("Media handled: ${intent.intentType}")
        }
    }

    @Test
    fun `router correctly dispatches known intent to registered handler`() = runTest {
        val appHandler = TestAppHandler()
        val mediaHandler = TestMediaHandler()
        val router = CommandRouter(setOf(appHandler, mediaHandler))

        val intent = CommandIntent(
            intentType = CommandIntentType.OPEN_APP,
            parameters = mapOf("app_name" to "Camera"),
            rawText = "open camera",
            confidence = 0.95f,
            source = CommandSource.VOICE
        )

        val result = router.route(intent)

        assertTrue(result.success)
        assertEquals("Opening Camera", result.message)
    }

    @Test
    fun `router returns not implemented for unregistered intent`() = runTest {
        val appHandler = TestAppHandler()
        val router = CommandRouter(setOf(appHandler))

        val intent = CommandIntent(
            intentType = CommandIntentType.YOUTUBE_PLAY,
            parameters = emptyMap(),
            rawText = "play jazz",
            confidence = 0.9f,
            source = CommandSource.VOICE
        )

        val result = router.route(intent)

        assertFalse(result.success)
        assertTrue(result.message.contains("coming in a future update"))
    }

    @Test
    fun `router handles UNKNOWN intent gracefully`() = runTest {
        val router = CommandRouter(emptySet())

        val intent = CommandIntent(
            intentType = CommandIntentType.UNKNOWN,
            parameters = emptyMap(),
            rawText = "some gibberish query",
            confidence = 0.1f,
            source = CommandSource.VOICE
        )

        val result = router.route(intent)

        assertFalse(result.success)
        assertTrue(result.message.contains("didn't understand"))
    }

    @Test
    fun `router reports canHandle accurately`() {
        val appHandler = TestAppHandler()
        val router = CommandRouter(setOf(appHandler))

        assertTrue(router.canHandle(CommandIntentType.OPEN_APP))
        assertTrue(router.canHandle(CommandIntentType.CLOSE_APP))
        assertFalse(router.canHandle(CommandIntentType.MEDIA_PLAY))
    }
}
