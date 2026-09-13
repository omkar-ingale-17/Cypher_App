package com.cypher.assistant.features.youtube

import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.youtube.domain.repository.YouTubeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YouTubeCommandHandlerTest {

    private lateinit var youTubeRepository: YouTubeRepository
    private lateinit var handler: YouTubeCommandHandler

    @Before
    fun setup() {
        youTubeRepository = mockk(relaxed = true)
        handler = YouTubeCommandHandler(youTubeRepository)
    }

    @Test
    fun supportedIntentsContainsAllSixteenIntents() {
        assertEquals(16, handler.supportedIntents.size)
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_SEARCH))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_PLAY_SEARCH))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_PAUSE))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_RESUME))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_STOP))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_NEXT))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_PREVIOUS))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_VOLUME_UP))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_VOLUME_DOWN))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_MUTE))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_UNMUTE))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_OPEN_HOME))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_OPEN_SHORTS))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_OPEN_SUBSCRIPTIONS))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_OPEN_HISTORY))
        assertTrue(handler.supportedIntents.contains(CommandIntentType.YOUTUBE_OPEN_CHANNEL))
    }

    @Test
    fun handleSearchDelegatesToRepository() = runTest {
        coEvery { youTubeRepository.search("Kotlin tutorials") } returns CommandResult.success("Searching YouTube for Kotlin tutorials.")

        val intent = CommandIntent(
            intentType = CommandIntentType.YOUTUBE_SEARCH,
            parameters = mapOf("query" to "Kotlin tutorials"),
            rawText = "search YouTube for Kotlin tutorials"
        )
        val result = handler.handle(intent)
        assertTrue(result.success)
        assertEquals("Searching YouTube for Kotlin tutorials.", result.message)
        coVerify { youTubeRepository.search("Kotlin tutorials") }
    }

    @Test
    fun handlePauseDelegatesToRepository() = runTest {
        coEvery { youTubeRepository.pause() } returns CommandResult.success("Paused.")

        val intent = CommandIntent(
            intentType = CommandIntentType.YOUTUBE_PAUSE,
            rawText = "pause YouTube"
        )
        val result = handler.handle(intent)
        assertTrue(result.success)
        assertEquals("Paused.", result.message)
        coVerify { youTubeRepository.pause() }
    }
}
