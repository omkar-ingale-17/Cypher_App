package com.cypher.assistant.core.command

/**
 * Exhaustive enumeration of every voice command intent Cypher can process.
 */
enum class CommandIntentType {

    // -- Conversational & Persona ------------------------------------------
    GREETING,
    ASSISTANT_NAME,
    ASSISTANT_STATUS,
    ASSISTANT_CAPABILITIES,
    SET_USER_NAME,
    GET_USER_NAME,
    GET_TIME,
    GET_DATE,
    THANK_YOU,
    GOODBYE,

    // -- App & System Navigation (Module 2) --------------------------------
    OPEN_APP,
    CLOSE_APP,
    LIST_APPS,
    MINIMIZE_APP,
    GO_HOME,
    GO_BACK,

    // -- YouTube Control (Module 3) ----------------------------------------
    YOUTUBE_SEARCH,
    YOUTUBE_PLAY_SEARCH,
    YOUTUBE_PLAY_INDEX,
    YOUTUBE_PAUSE,
    YOUTUBE_RESUME,
    YOUTUBE_STOP,
    YOUTUBE_NEXT,
    YOUTUBE_PREVIOUS,
    YOUTUBE_LIKE,
    YOUTUBE_DISLIKE,
    YOUTUBE_SUBSCRIBE,
    YOUTUBE_UNSUBSCRIBE,
    YOUTUBE_COMMENTS_OPEN,
    YOUTUBE_COMMENTS_CLOSE,
    YOUTUBE_DESCRIPTION_OPEN,
    YOUTUBE_SHOW_MORE,
    YOUTUBE_SHOW_LESS,
    YOUTUBE_SCROLL_UP,
    YOUTUBE_SCROLL_DOWN,
    YOUTUBE_OPEN_HOME,
    YOUTUBE_OPEN_SHORTS,
    YOUTUBE_OPEN_SUBSCRIPTIONS,
    YOUTUBE_OPEN_HISTORY,
    YOUTUBE_OPEN_CHANNEL,
    YOUTUBE_OPEN_NOTIFICATIONS,
    YOUTUBE_GO_BACK,
    YOUTUBE_VOLUME_UP,
    YOUTUBE_VOLUME_DOWN,
    YOUTUBE_MUTE,
    YOUTUBE_UNMUTE,

    // -- Telephony ---------------------------------------------------------
    CALL_CONTACT,
    DIAL_NUMBER,
    END_CALL,

    // -- Messaging ---------------------------------------------------------
    SEND_SMS,
    READ_SMS,

    // -- Media Playback ----------------------------------------------------
    MEDIA_PLAY,
    MEDIA_PAUSE,
    MEDIA_NEXT,
    MEDIA_PREV,
    MEDIA_STOP,

    // -- Volume ------------------------------------------------------------
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    VOLUME_UNMUTE,
    VOLUME_SET,

    // -- System Settings ---------------------------------------------------
    OPEN_SETTINGS,
    TOGGLE_WIFI,
    TOGGLE_BLUETOOTH,
    TOGGLE_FLASHLIGHT,
    SET_BRIGHTNESS,
    SET_ALARM,
    SET_TIMER,

    // -- Web ---------------------------------------------------------------
    WEB_SEARCH,
    OPEN_URL,

    // -- Notifications -----------------------------------------------------
    READ_NOTIFICATION,
    DISMISS_NOTIFICATION,
    REPLY_NOTIFICATION,

    // -- Security & Device Control -----------------------------------------
    LOCK_SCREEN,
    BIOMETRIC_AUTH,

    // -- Fallback ----------------------------------------------------------
    UNKNOWN
}
