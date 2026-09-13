package com.cypher.assistant.features.youtube.accessibility

/**
 * Semantic constants, text strings, content descriptions, and resource ID patterns for YouTube UI.
 */
object YouTubeSelectors {
    val SEARCH_BUTTON_DESCRIPTIONS = listOf(
        "Search", "Search YouTube", "Search...", "Rechercher", "Buscar", "Suche"
    )

    val SEARCH_INPUT_IDS = listOf(
        "search_edit_text", "search_box", "search_input", "search_src_text"
    )

    val LIKE_BUTTON_DESCRIPTIONS = listOf(
        "like this video along with",
        "like this video",
        "like",
        "i like this",
        "like button"
    )

    val LIKED_BUTTON_DESCRIPTIONS = listOf(
        "unlike",
        "liked",
        "remove like",
        "remove like from this video",
        "liked video"
    )

    val DISLIKE_BUTTON_DESCRIPTIONS = listOf(
        "dislike this video",
        "dislike",
        "i dislike this",
        "dislike button"
    )

    val SUBSCRIBE_BUTTON_TEXTS = listOf(
        "subscribe", "s'abonner", "suscribirse", "abonnieren"
    )

    val SUBSCRIBED_BUTTON_TEXTS = listOf(
        "subscribed", "abonné", "suscrito", "abonniert", "unsubscribe"
    )

    val COMMENTS_SECTION_PATTERNS = listOf(
        "comments", "commentaires", "comentarios", "kommentare"
    )

    val CLOSE_COMMENTS_DESCRIPTIONS = listOf(
        "close", "dismiss", "back", "close comments"
    )

    val SHOW_MORE_TEXTS = listOf(
        "more", "show more", "...more", "plus", "más", "mehr"
    )

    val SHOW_LESS_TEXTS = listOf(
        "show less", "less", "moins", "menos", "weniger"
    )

    val NAVIGATION_TABS = mapOf(
        "home" to listOf("Home", "Accueil", "Inicio", "Startseite"),
        "shorts" to listOf("Shorts", "Court"),
        "subscriptions" to listOf("Subscriptions", "Abonnements", "Suscripciones", "Abos"),
        "library" to listOf("Library", "You", "Bibliothèque", "Biblioteca", "Mediathek"),
        "notifications" to listOf("Notifications", "Benachrichtigungen")
    )
}
