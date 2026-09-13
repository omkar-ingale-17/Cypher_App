package com.cypher.assistant.features.youtube.data

import android.content.Context
import android.util.Log
import com.cypher.assistant.features.youtube.accessibility.YouTubePackageResolver
import com.cypher.assistant.features.youtube.accessibility.YouTubeScreenDetector
import com.cypher.assistant.features.youtube.accessibility.YouTubeUiMatcher
import com.cypher.assistant.features.youtube.domain.model.YouTubeActionResult
import com.cypher.assistant.features.youtube.domain.model.YouTubeCommand
import com.cypher.assistant.features.youtube.domain.model.YouTubeScreenType
import com.cypher.assistant.features.youtube.domain.repository.YouTubeRepository
import com.cypher.assistant.services.accessibility.CypherAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [YouTubeRepository].
 * Coordinates Official Intents, Public MediaSession/AudioManager, and Safe Accessibility UI Automation.
 */
@Singleton
class YouTubeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appResolver: YouTubeAppResolver,
    private val mediaDataSource: YouTubeMediaDataSource,
    private val intentDataSource: YouTubeIntentDataSource,
    private val uiMatcher: YouTubeUiMatcher,
    private val screenDetector: YouTubeScreenDetector
) : YouTubeRepository {

    companion object {
        private const val TAG = "YouTubeRepoImpl"
        private const val UI_TIMEOUT_MS = 3500L
    }

    private val _currentScreen = MutableStateFlow(YouTubeScreenType.UNKNOWN)
    override val currentScreen: StateFlow<YouTubeScreenType> = _currentScreen.asStateFlow()

    override val isYouTubeInstalled: Boolean
        get() = appResolver.isYouTubeInstalled()

    override val isAccessibilityEnabled: Boolean
        get() = CypherAccessibilityService.isServiceRunning()

    override fun getInstalledPackage(): String? = appResolver.getYouTubePackage()

    override fun openAccessibilitySettings() {
        CypherAccessibilityService.openAccessibilitySettings(context)
    }

    override suspend fun execute(command: YouTubeCommand): YouTubeActionResult {
        Log.i(TAG, "Executing YouTubeCommand: \$command")

        if (!isYouTubeInstalled) {
            return YouTubeActionResult.NotFound("YouTube", "YouTube application is not installed on this device.")
        }

        return when (command) {
            is YouTubeCommand.Open -> handleOpen()
            is YouTubeCommand.Search -> handleSearch(command.query)
            is YouTubeCommand.PlaySearch -> handlePlaySearch(command.query)
            is YouTubeCommand.PlayIndex -> handlePlayIndex(command.index)
            is YouTubeCommand.Pause -> handlePause()
            is YouTubeCommand.Resume -> handleResume()
            is YouTubeCommand.Stop -> handleStop()
            is YouTubeCommand.Next -> handleNext()
            is YouTubeCommand.Previous -> handlePrevious()
            is YouTubeCommand.Like -> handleLike()
            is YouTubeCommand.Dislike -> handleDislike()
            is YouTubeCommand.Subscribe -> handleSubscribe()
            is YouTubeCommand.Unsubscribe -> handleUnsubscribe(command.confirmed)
            is YouTubeCommand.OpenComments -> handleOpenComments()
            is YouTubeCommand.CloseComments -> handleCloseComments()
            is YouTubeCommand.OpenDescription -> handleOpenDescription()
            is YouTubeCommand.ShowMore -> handleShowMore()
            is YouTubeCommand.ShowLess -> handleShowLess()
            is YouTubeCommand.ScrollUp -> handleScrollUp()
            is YouTubeCommand.ScrollDown -> handleScrollDown()
            is YouTubeCommand.OpenHome -> handleOpenHome()
            is YouTubeCommand.OpenShorts -> handleOpenShorts()
            is YouTubeCommand.OpenSubscriptions -> handleOpenSubscriptions()
            is YouTubeCommand.OpenHistory -> handleOpenHistory()
            is YouTubeCommand.OpenChannel -> handleOpenChannel()
            is YouTubeCommand.OpenNotifications -> handleOpenNotifications()
            is YouTubeCommand.GoBack -> handleGoBack()
            is YouTubeCommand.VolumeUp -> handleVolumeUp()
            is YouTubeCommand.VolumeDown -> handleVolumeDown()
            is YouTubeCommand.Mute -> handleMute()
            is YouTubeCommand.Unmute -> handleUnmute()
        }
    }

    // --- Intent & Navigation Handlers ---

    private fun handleOpen(): YouTubeActionResult {
        val launchIntent = appResolver.getLaunchIntent()
            ?: return YouTubeActionResult.Failed("Could not create launch intent for YouTube.")
        return try {
            context.startActivity(launchIntent)
            YouTubeActionResult.Success("Opening YouTube.")
        } catch (e: Exception) {
            YouTubeActionResult.Failed("Failed to launch YouTube: \${e.message}")
        }
    }

    private fun handleSearch(query: String): YouTubeActionResult {
        if (query.isBlank()) {
            return YouTubeActionResult.Failed("What would you like me to search on YouTube?")
        }
        val success = intentDataSource.launchSearch(query)
        return if (success) {
            YouTubeActionResult.Success("Searching YouTube for \$query.")
        } else {
            YouTubeActionResult.Failed("Could not search YouTube for \$query.")
        }
    }

    private suspend fun handlePlaySearch(query: String): YouTubeActionResult {
        if (query.isBlank()) {
            return YouTubeActionResult.Failed("What video would you like me to play?")
        }

        // 1. Launch search via official intent
        val launched = intentDataSource.launchSearch(query)
        if (!launched) {
            return YouTubeActionResult.Failed("Could not open YouTube search for \$query.")
        }

        // 2. If accessibility is not enabled, inform user that search opened
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.Success("Found search results for \$query on YouTube.")
        }

        // 3. Wait for search results and select the best candidate
        return withTimeoutOrNull(UI_TIMEOUT_MS) {
            delay(1200L) // Allow UI transition
            val root = CypherAccessibilityService.getRootNode()
            if (root == null) {
                return@withTimeoutOrNull YouTubeActionResult.Success("Searching YouTube for \$query.")
            }

            val candidates = uiMatcher.findVideoCandidates(root, query)
            if (candidates.isNotEmpty()) {
                val best = candidates.first()
                if (best.node != null && CypherAccessibilityService.clickNode(best.node)) {
                    YouTubeActionResult.Success("Playing \${best.title.take(45)}.")
                } else {
                    YouTubeActionResult.Success("Found search results for \$query.")
                }
            } else {
                YouTubeActionResult.Success("Found search results for \$query.")
            }
        } ?: YouTubeActionResult.Success("Searching YouTube for \$query.")
    }

    private suspend fun handlePlayIndex(index: Int): YouTubeActionResult {
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Accessibility access is required to select video items on screen.")
        }

        val root = CypherAccessibilityService.getRootNode()
            ?: return YouTubeActionResult.NotFound("Video list", "Could not read YouTube screen content.")

        val candidates = uiMatcher.findVideoCandidates(root)
        if (candidates.isEmpty()) {
            return YouTubeActionResult.NotFound("Videos", "I couldn't find any visible video results on screen.")
        }

        val targetIdx = index - 1
        if (targetIdx in candidates.indices) {
            val target = candidates[targetIdx]
            if (target.node != null && CypherAccessibilityService.clickNode(target.node)) {
                return YouTubeActionResult.Success("Playing \${target.title.take(45)}.")
            }
        }

        return YouTubeActionResult.NotFound("Video #\$index", "I couldn't safely identify video number \$index.")
    }

    // --- Media Playback Handlers ---

    private fun handlePause(): YouTubeActionResult {
        val success = mediaDataSource.pause()
        return if (success) YouTubeActionResult.Success("Paused.") else YouTubeActionResult.Failed("Could not pause playback.")
    }

    private fun handleResume(): YouTubeActionResult {
        val success = mediaDataSource.resume()
        return if (success) YouTubeActionResult.Success("Resumed.") else YouTubeActionResult.Failed("Could not resume playback.")
    }

    private fun handleStop(): YouTubeActionResult {
        val success = mediaDataSource.stop()
        return if (success) YouTubeActionResult.Success("Stopped.") else YouTubeActionResult.Failed("Could not stop playback.")
    }

    private fun handleNext(): YouTubeActionResult {
        val success = mediaDataSource.next()
        return if (success) YouTubeActionResult.Success("Playing next video.") else YouTubeActionResult.Failed("Could not skip to next video.")
    }

    private fun handlePrevious(): YouTubeActionResult {
        val success = mediaDataSource.previous()
        return if (success) YouTubeActionResult.Success("Playing previous video.") else YouTubeActionResult.Failed("Could not go to previous video.")
    }

    // --- Accessibility UI Automation Handlers ---

    private suspend fun handleLike(): YouTubeActionResult {
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service in Settings to like videos.")
        }

        val root = CypherAccessibilityService.getRootNode()
            ?: return YouTubeActionResult.NotFound("Like button", "Could not access YouTube screen.")

        val (likeNode, isAlreadyLiked) = uiMatcher.findLikeButton(root)
        if (isAlreadyLiked) {
            return YouTubeActionResult.AlreadyInState("This video is already liked.")
        }

        if (likeNode != null) {
            val clicked = CypherAccessibilityService.clickNode(likeNode)
            return if (clicked) {
                YouTubeActionResult.Success("Liked.")
            } else {
                YouTubeActionResult.Failed("Could not click the Like button.")
            }
        }

        return YouTubeActionResult.NotFound("Like button", "I couldn't find the Like button on this screen.")
    }

    private suspend fun handleDislike(): YouTubeActionResult {
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service in Settings to dislike videos.")
        }

        val root = CypherAccessibilityService.getRootNode()
            ?: return YouTubeActionResult.NotFound("Dislike button", "Could not access YouTube screen.")

        val dislikeNode = uiMatcher.findDislikeButton(root)
        if (dislikeNode != null) {
            val clicked = CypherAccessibilityService.clickNode(dislikeNode)
            return if (clicked) {
                YouTubeActionResult.Success("Disliked.")
            } else {
                YouTubeActionResult.Failed("Could not click the Dislike button.")
            }
        }

        return YouTubeActionResult.NotFound("Dislike button", "I couldn't find the Dislike button on this screen.")
    }

    private suspend fun handleSubscribe(): YouTubeActionResult {
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service in Settings to subscribe to channels.")
        }

        val root = CypherAccessibilityService.getRootNode()
            ?: return YouTubeActionResult.NotFound("Subscribe button", "Could not access YouTube screen.")

        val (subNode, isAlreadySubscribed) = uiMatcher.findSubscribeButton(root)
        if (isAlreadySubscribed) {
            return YouTubeActionResult.AlreadyInState("You're already subscribed to this channel.")
        }

        if (subNode != null) {
            val clicked = CypherAccessibilityService.clickNode(subNode)
            return if (clicked) {
                YouTubeActionResult.Success("Subscribed.")
            } else {
                YouTubeActionResult.Failed("Could not click the Subscribe button.")
            }
        }

        return YouTubeActionResult.NotFound("Subscribe button", "I couldn't find the Subscribe button on this screen.")
    }

    private suspend fun handleUnsubscribe(confirmed: Boolean): YouTubeActionResult {
        if (!confirmed) {
            return YouTubeActionResult.ConfirmationRequired("You are currently subscribed. Do you want me to unsubscribe?")
        }

        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service in Settings to manage subscriptions.")
        }

        val root = CypherAccessibilityService.getRootNode()
            ?: return YouTubeActionResult.NotFound("Subscription button", "Could not access YouTube screen.")

        val (subNode, isAlreadySubscribed) = uiMatcher.findSubscribeButton(root)
        if (!isAlreadySubscribed) {
            return YouTubeActionResult.AlreadyInState("You are not subscribed to this channel.")
        }

        if (subNode != null) {
            val clicked = CypherAccessibilityService.clickNode(subNode)
            return if (clicked) {
                YouTubeActionResult.Success("Unsubscribed.")
            } else {
                YouTubeActionResult.Failed("Could not click unsubscribe.")
            }
        }

        return YouTubeActionResult.NotFound("Subscription button", "I couldn't find the subscription control on this screen.")
    }

    private suspend fun handleOpenComments(): YouTubeActionResult {
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service in Settings to open comments.")
        }

        val root = CypherAccessibilityService.getRootNode()
            ?: return YouTubeActionResult.NotFound("Comments", "Could not access YouTube screen.")

        val commentsNode = uiMatcher.findCommentsSection(root)
        if (commentsNode != null) {
            val clicked = CypherAccessibilityService.clickNode(commentsNode)
            return if (clicked) {
                YouTubeActionResult.Success("Opening comments.")
            } else {
                YouTubeActionResult.Failed("Could not open comments.")
            }
        }

        return YouTubeActionResult.NotFound("Comments", "I couldn't find the comments section on this screen.")
    }

    private suspend fun handleCloseComments(): YouTubeActionResult {
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service in Settings to close comments.")
        }

        val root = CypherAccessibilityService.getRootNode()
            ?: return YouTubeActionResult.NotFound("Close button", "Could not access YouTube screen.")

        val closeBtn = uiMatcher.findCloseCommentsButton(root)
        if (closeBtn != null) {
            val clicked = CypherAccessibilityService.clickNode(closeBtn)
            return if (clicked) {
                YouTubeActionResult.Success("Closed comments.")
            } else {
                YouTubeActionResult.Failed("Could not close comments.")
            }
        }

        // Fallback to accessibility back
        val backed = CypherAccessibilityService.goBack()
        return if (backed) YouTubeActionResult.Success("Closed comments.") else YouTubeActionResult.NotFound("Close button", "Could not dismiss comments.")
    }

    private suspend fun handleOpenDescription(): YouTubeActionResult {
        return handleShowMore()
    }

    private suspend fun handleShowMore(): YouTubeActionResult {
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service in Settings.")
        }

        val root = CypherAccessibilityService.getRootNode()
            ?: return YouTubeActionResult.NotFound("Show more", "Could not access YouTube screen.")

        val moreBtn = uiMatcher.findShowMoreButton(root)
        if (moreBtn != null) {
            val clicked = CypherAccessibilityService.clickNode(moreBtn)
            return if (clicked) YouTubeActionResult.Success("Expanded description.") else YouTubeActionResult.Failed("Could not expand description.")
        }

        return YouTubeActionResult.NotFound("Show more", "I couldn't find a Show More button on this screen.")
    }

    private suspend fun handleShowLess(): YouTubeActionResult {
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service in Settings.")
        }

        val root = CypherAccessibilityService.getRootNode()
            ?: return YouTubeActionResult.NotFound("Show less", "Could not access YouTube screen.")

        val lessBtn = uiMatcher.findShowLessButton(root)
        if (lessBtn != null) {
            val clicked = CypherAccessibilityService.clickNode(lessBtn)
            return if (clicked) YouTubeActionResult.Success("Collapsed description.") else YouTubeActionResult.Failed("Could not collapse description.")
        }

        return YouTubeActionResult.NotFound("Show less", "I couldn't find a Show Less button on this screen.")
    }

    private fun handleScrollUp(): YouTubeActionResult {
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service in Settings to scroll.")
        }
        val scrolled = CypherAccessibilityService.scrollBackward()
        return if (scrolled) YouTubeActionResult.Success("Scrolled up.") else YouTubeActionResult.Failed("Could not scroll up.")
    }

    private fun handleScrollDown(): YouTubeActionResult {
        if (!isAccessibilityEnabled) {
            return YouTubeActionResult.PermissionRequired("Please enable Cypher's Accessibility service in Settings to scroll.")
        }
        val scrolled = CypherAccessibilityService.scrollForward()
        return if (scrolled) YouTubeActionResult.Success("Scrolled down.") else YouTubeActionResult.Failed("Could not scroll down.")
    }

    private fun handleOpenHome(): YouTubeActionResult {
        val launched = intentDataSource.launchHome()
        return if (launched) YouTubeActionResult.Success("Going to YouTube home.") else YouTubeActionResult.Failed("Could not open YouTube home.")
    }

    private fun handleOpenShorts(): YouTubeActionResult {
        val launched = intentDataSource.launchShorts()
        return if (launched) YouTubeActionResult.Success("Opening YouTube Shorts.") else YouTubeActionResult.Failed("Could not open Shorts.")
    }

    private fun handleOpenSubscriptions(): YouTubeActionResult {
        val launched = intentDataSource.launchSubscriptions()
        return if (launched) YouTubeActionResult.Success("Opening YouTube subscriptions.") else YouTubeActionResult.Failed("Could not open subscriptions.")
    }

    private fun handleOpenHistory(): YouTubeActionResult {
        val launched = intentDataSource.launchHistory()
        return if (launched) YouTubeActionResult.Success("Opening YouTube history.") else YouTubeActionResult.Failed("Could not open history.")
    }

    private fun handleOpenChannel(): YouTubeActionResult {
        val pkg = appResolver.getYouTubePackage() ?: return YouTubeActionResult.NotFound("YouTube", "YouTube is not installed.")
        val intent = appResolver.getLaunchIntent() ?: return YouTubeActionResult.Failed("Could not open YouTube.")
        context.startActivity(intent)
        return YouTubeActionResult.Success("Opening YouTube channel page.")
    }

    private fun handleOpenNotifications(): YouTubeActionResult {
        val launched = intentDataSource.launchNotifications()
        return if (launched) YouTubeActionResult.Success("Opening YouTube notifications.") else YouTubeActionResult.Failed("Could not open notifications.")
    }

    private fun handleGoBack(): YouTubeActionResult {
        val backed = CypherAccessibilityService.goBack()
        return if (backed) YouTubeActionResult.Success("Going back.") else YouTubeActionResult.Failed("Could not navigate back.")
    }

    private fun handleVolumeUp(): YouTubeActionResult {
        mediaDataSource.increaseVolume()
        return YouTubeActionResult.Success("Volume increased.")
    }

    private fun handleVolumeDown(): YouTubeActionResult {
        mediaDataSource.decreaseVolume()
        return YouTubeActionResult.Success("Volume decreased.")
    }

    private fun handleMute(): YouTubeActionResult {
        mediaDataSource.mute()
        return YouTubeActionResult.Success("Muted.")
    }

    private fun handleUnmute(): YouTubeActionResult {
        mediaDataSource.unmute()
        return YouTubeActionResult.Success("Unmuted.")
    }
}
