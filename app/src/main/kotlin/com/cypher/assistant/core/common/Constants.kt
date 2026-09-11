package com.cypher.assistant.core.common

object Constants {
    const val APP_NAME = "Cypher"

    // ── Database ──────────────────────────────────────────────────────────────
    const val DATABASE_NAME       = "cypher_database"
    const val DATASTORE_NAME      = "cypher_preferences"

    // ── NLU confidence thresholds ─────────────────────────────────────────────
    const val CONFIDENCE_HIGH     = 0.85f
    const val CONFIDENCE_MEDIUM   = 0.60f
    const val CONFIDENCE_LOW      = 0.40f

    // ── TTS ───────────────────────────────────────────────────────────────────
    const val TTS_UTTERANCE_PREFIX = "cypher_"

    // ── History ───────────────────────────────────────────────────────────────
    const val MAX_COMMAND_HISTORY = 100

    // ── Notification channel IDs ──────────────────────────────────────────────
    const val NOTIF_CHANNEL_VOICE   = "cypher_voice_service"
    const val NOTIF_CHANNEL_GENERAL = "cypher_general"

    // ── Intent parameter keys (used by CommandIntent.parameters map) ──────────
    object Params {
        const val APP_NAME         = "app_name"
        const val APP_PACKAGE      = "app_package"
        const val CONTACT_NAME     = "contact_name"
        const val PHONE_NUMBER     = "phone_number"
        const val SMS_BODY         = "sms_body"
        const val SEARCH_QUERY     = "search_query"
        const val URL              = "url"
        const val MEDIA_TRACK      = "media_track"
        const val VOLUME_LEVEL     = "volume_level"
        const val BRIGHTNESS_LEVEL = "brightness_level"
        const val ALARM_HOUR       = "alarm_hour"
        const val ALARM_MINUTE     = "alarm_minute"
        const val ALARM_LABEL      = "alarm_label"
        const val TIMER_SECONDS    = "timer_seconds"
        const val NOTIFICATION_KEY = "notification_key"
    }
}
