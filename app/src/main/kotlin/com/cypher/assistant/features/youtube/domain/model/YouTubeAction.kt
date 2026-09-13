package com.cypher.assistant.features.youtube.domain.model

/**
 * Sealed hierarchy of all YouTube actions Cypher can dispatch.
 */
sealed interface YouTubeAction {
    data object OpenHome           : YouTubeAction
    data class  Search(val query: String) : YouTubeAction
    data class  PlaySearch(val query: String) : YouTubeAction
    data object Pause              : YouTubeAction
    data object Resume             : YouTubeAction
    data object Stop               : YouTubeAction
    data object Next               : YouTubeAction
    data object Previous           : YouTubeAction
    data object VolumeUp           : YouTubeAction
    data object VolumeDown         : YouTubeAction
    data object Mute               : YouTubeAction
    data object Unmute             : YouTubeAction
    data object OpenShorts         : YouTubeAction
    data object OpenSubscriptions  : YouTubeAction
    data object OpenHistory        : YouTubeAction
    data object OpenChannel        : YouTubeAction
}
