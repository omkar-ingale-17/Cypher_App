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

    // -- App Control -------------------------------------------------------
    OPEN_APP,
    CLOSE_APP,
    LIST_APPS,

    // -- Telephony ---------------------------------------------------------
    CALL_CONTACT,
    DIAL_NUMBER,
    END_CALL,

    // -- Messaging ---------------------------------------------------------
    SEND_SMS,
    READ_SMS,

    // -- YouTube -----------------------------------------------------------
    YOUTUBE_SEARCH,
    YOUTUBE_PLAY,

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

    // -- Security ----------------------------------------------------------
    LOCK_SCREEN,
    BIOMETRIC_AUTH,

    // -- Fallback ----------------------------------------------------------
    UNKNOWN
}
