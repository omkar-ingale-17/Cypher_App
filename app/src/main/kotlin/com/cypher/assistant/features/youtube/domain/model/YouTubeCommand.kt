package com.cypher.assistant.features.youtube.domain.model

/**
 * High-level domain command representation for YouTube operations.
 */
sealed interface YouTubeCommand {
    data object Open : YouTubeCommand
    data class Search(val query: String) : YouTubeCommand
    data class PlaySearch(val query: String) : YouTubeCommand
    data class PlayIndex(val index: Int) : YouTubeCommand
    data object Pause : YouTubeCommand
    data object Resume : YouTubeCommand
    data object Stop : YouTubeCommand
    data object Next : YouTubeCommand
    data object Previous : YouTubeCommand
    data object Like : YouTubeCommand
    data object Dislike : YouTubeCommand
    data object Subscribe : YouTubeCommand
    data class Unsubscribe(val confirmed: Boolean = false) : YouTubeCommand
    data object OpenComments : YouTubeCommand
    data object CloseComments : YouTubeCommand
    data object OpenDescription : YouTubeCommand
    data object ShowMore : YouTubeCommand
    data object ShowLess : YouTubeCommand
    data object ScrollUp : YouTubeCommand
    data object ScrollDown : YouTubeCommand
    data object OpenHome : YouTubeCommand
    data object OpenShorts : YouTubeCommand
    data object OpenSubscriptions : YouTubeCommand
    data object OpenHistory : YouTubeCommand
    data object OpenChannel : YouTubeCommand
    data object OpenNotifications : YouTubeCommand
    data object GoBack : YouTubeCommand
    data object VolumeUp : YouTubeCommand
    data object VolumeDown : YouTubeCommand
    data object Mute : YouTubeCommand
    data object Unmute : YouTubeCommand
}
