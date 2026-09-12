package com.cypher.assistant.core.voice

/**
 * Operational states of the Cypher Voice Engine reflecting a two-stage voice pipeline:
 *
 * STAGE 1: Wake-word listening
 * - IDLE
 * - LISTENING_FOR_WAKE_WORD
 * - WAKE_WORD_DETECTED
 *
 * STAGE 2: Command listening & execution
 * - LISTENING_FOR_COMMAND
 * - PROCESSING
 * - SPEAKING
 * - ERROR
 */
enum class VoiceState {
    IDLE,
    LISTENING_FOR_WAKE_WORD,
    WAKE_WORD_DETECTED,
    LISTENING_FOR_COMMAND,
    PROCESSING,
    SPEAKING,
    ERROR
}
