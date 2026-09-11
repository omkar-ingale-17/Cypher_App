package com.cypher.assistant.core.command

/**
 * Exhaustive enumeration of every voice command intent Cypher can process.
 *
 * Design rules:
 *  - Add new intents here when a new feature handler is introduced.
 *  - Group by feature domain for readability.
 *  - Mark intents with Android API restrictions in comments.
 */
enum class CommandIntentType {

    // ── App Control ───────────────────────────────────────────────────────────
    OPEN_APP,
    CLOSE_APP,     // Requires FORCE_STOP_PACKAGES (system) — handled via back-press simulation
    LIST_APPS,

    // ── Telephony ─────────────────────────────────────────────────────────────
    CALL_CONTACT,  // CALL_PHONE permission required
    DIAL_NUMBER,   // No permission — opens dialer
    END_CALL,      // ANSWER_PHONE_CALLS + TelecomManager.endCall() (API 28+)

    // ── Messaging ─────────────────────────────────────────────────────────────
    SEND_SMS,      // SEND_SMS permission required
    READ_SMS,      // READ_SMS permission required

    // ── YouTube ───────────────────────────────────────────────────────────────
    YOUTUBE_SEARCH,
    YOUTUBE_PLAY,

    // ── Media Playback ────────────────────────────────────────────────────────
    MEDIA_PLAY,
    MEDIA_PAUSE,
    MEDIA_NEXT,
    MEDIA_PREV,
    MEDIA_STOP,

    // ── Volume ────────────────────────────────────────────────────────────────
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    VOLUME_UNMUTE,
    VOLUME_SET,

    // ── System Settings ───────────────────────────────────────────────────────
    OPEN_SETTINGS,
    TOGGLE_WIFI,        // API 29+: shows Settings panel (direct toggle removed by Android)
    TOGGLE_BLUETOOTH,   // API 33+: shows Settings panel (direct toggle removed by Android)
    TOGGLE_FLASHLIGHT,  // CameraManager — works without special permissions
    SET_BRIGHTNESS,     // WRITE_SETTINGS required; user must grant in Special App Access
    SET_ALARM,          // Intent ACTION_SET_ALARM — no permission needed
    SET_TIMER,          // Intent ACTION_SET_TIMER — no permission needed

    // ── Web ───────────────────────────────────────────────────────────────────
    WEB_SEARCH,
    OPEN_URL,

    // ── Notifications ─────────────────────────────────────────────────────────
    READ_NOTIFICATION,      // NotificationListenerService required
    DISMISS_NOTIFICATION,   // NotificationListenerService required
    REPLY_NOTIFICATION,     // NotificationListenerService required (Stage 12)

    // ── Security ──────────────────────────────────────────────────────────────
    LOCK_SCREEN,       // DevicePolicyManager.lockNow() requires BIND_DEVICE_ADMIN
    BIOMETRIC_AUTH,    // BiometricPrompt — no special permission

    // ── Fallback ──────────────────────────────────────────────────────────────
    UNKNOWN
}
