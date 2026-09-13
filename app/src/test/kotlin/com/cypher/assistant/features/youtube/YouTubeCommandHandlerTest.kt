package com.cypher.assistant.features.youtube

import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.features.youtube.domain.model.YouTubeActionResult
import com.cypher.assistant.features.youtube.domain.model.YouTubeCommand
import com.cypher.assistant.features.youtube.domain.model.YouTubeScreenType
import com.cypher.assistant.features.youtube.domain.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YouTubeCommandHandlerTest {

    private lateinit var fakeRepository: FakeYouTubeRepository
    private lateinit var handler: YouTubeCommandHandler

    @Before
    fun setup() {
        fakeRepository = FakeYouTubeRepository()
        handler = YouTubeCommandHandler(fakeRepository)
    }

    @Test
    fun handlesSearchIntentSuccessfully() = runBlocking {
        fakeRepository.nextResult = YouTubeActionResult.Success("Searching YouTube for Python.")
        val intent = CommandIntent(
            intentType = CommandIntentType.YOUTUBE_SEARCH,
            parameters = mapOf("query" to "Python"),
            rawText = "search YouTube for Python"
        )
        val result = handler.handle(intent)
        assertTrue(result.success)
        assertEquals("Searching YouTube for Python.", result.message)
    }

    @Test
    fun handlesPauseIntentSuccessfully() = runBlocking {
        fakeRepository.nextResult = YouTubeActionResult.Success("Paused.")
        val intent = CommandIntent(
            intentType = CommandIntentType.YOUTUBE_PAUSE,
            parameters = emptyMap(),
            rawText = "pause"
        )
        val result = handler.handle(intent)
        assertTrue(result.success)
        assertEquals("Paused.", result.message)
    }

    @Test
    fun handlesAlreadyLikedState() = runBlocking {
        fakeRepository.nextResult = YouTubeActionResult.AlreadyInState("This video is already liked.")
        val intent = CommandIntent(
            intentType = CommandIntentType.YOUTUBE_LIKE,
            parameters = emptyMap(),
            rawText = "like this video"
        )
        val result = handler.handle(intent)
        assertTrue(result.success)
        assertEquals("This video is already liked.", result.message)
    }

    @Test
    fun handlesUnsubscribeConfirmationRequired() = runBlocking {
        fakeRepository.nextResult = YouTubeActionResult.ConfirmationRequired("You are currently subscribed. Do you want me to unsubscribe?")
        val intent = CommandIntent(
            intentType = CommandIntentType.YOUTUBE_UNSUBSCRIBE,
            parameters = emptyMap(),
            rawText = "unsubscribe",
            requiresConfirmation = true
        )
        val result = handler.handle(intent)
        assertTrue(result.success)
        assertTrue(result.message.contains("unsubscribe"))
        assertEquals(true, result.data["requiresConfirmation"])
    }

    @Test
    fun handlesAccessibilityDisabledGracefully() = runBlocking {
        fakeRepository.nextResult = YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service.")
        val intent = CommandIntent(
            intentType = CommandIntentType.YOUTUBE_COMMENTS_OPEN,
            parameters = emptyMap(),
            rawText = "open comments"
        )
        val result = handler.handle(intent)
        assertFalse(result.success)
        assertTrue(result.message.contains("Accessibility"))
    }

    private class FakeYouTubeRepository : YouTubeRepository {
        var nextResult: YouTubeActionResult = YouTubeActionResult.Success("OK")
        override val currentScreen: StateFlow<YouTubeScreenType> = MutableStateFlow(YouTubeScreenType.VIDEO)
        override val isYouTubeInstalled: Boolean = true
        override val isAccessibilityEnabled: Boolean = true

        override suspend fun execute(command: YouTubeCommand): YouTubeActionResult = nextResult
        override fun getInstalledPackage(): String? = "com.google.android.youtube"
        override fun openAccessibilitySettings() {}
    }
}
