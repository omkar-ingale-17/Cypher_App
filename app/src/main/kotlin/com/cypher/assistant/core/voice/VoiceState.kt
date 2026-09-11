package com.cypher.assistant.core.voice

/**
 * High-level operational states of the Cypher Voice Engine.
 *
 * State flow:
 * IDLE → (tap mic / wake phrase) → LISTENING → (speech detected) → PROCESSING
 * → (command executed) → SPEAKING → (speech finished) → IDLE (or LISTENING if continuous)
 *
 * Any unexpected error transitions to ERROR with a user-friendly message.
 */
enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}
