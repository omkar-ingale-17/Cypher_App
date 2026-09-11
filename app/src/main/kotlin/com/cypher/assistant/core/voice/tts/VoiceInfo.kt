package com.cypher.assistant.core.voice.tts

import java.util.Locale

/**
 * Metadata representation of an available Text-to-Speech voice.
 */
data class VoiceInfo(
    val name: String,
    val displayName: String,
    val locale: Locale,
    val isFemale: Boolean?,
    val quality: Int = 300,
    val latency: Int = 300,
    val requiresNetwork: Boolean = false
)
