package com.cypher.assistant.core.voice.wake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WakeWordDetectorTest {

    private lateinit var detector: WakeWordDetector

    @Before
    fun setup() {
        detector = WakeWordDetector()
    }

    @Test
    fun `detects wake only when user says cypher`() {
        val result = detector.process("Cypher")
        assertEquals(WakePhraseResult.WakeOnly, result)
    }

    @Test
    fun `detects wake only with hey cypher with punctuation`() {
        val result = detector.process("Hey Cypher!")
        assertEquals(WakePhraseResult.WakeOnly, result)
    }

    @Test
    fun `detects wake with command payload when prefixed with cypher`() {
        val result = detector.process("Cypher, open WhatsApp")
        assertTrue(result is WakePhraseResult.WakeWithCommand)
        assertEquals("open whatsapp", (result as WakePhraseResult.WakeWithCommand).commandPayload)
    }

    @Test
    fun `detects wake with command payload when prefixed with ok cypher`() {
        val result = detector.process("Ok Cypher, play jazz on YouTube")
        assertTrue(result is WakePhraseResult.WakeWithCommand)
        assertEquals("play jazz on youtube", (result as WakePhraseResult.WakeWithCommand).commandPayload)
    }

    @Test
    fun `returns direct command when requireWakePhrase is false`() {
        val result = detector.process("Turn on flashlight", requireWakePhrase = false)
        assertTrue(result is WakePhraseResult.DirectCommand)
        assertEquals("turn on flashlight", (result as WakePhraseResult.DirectCommand).commandPayload)
    }

    @Test
    fun `returns None when requireWakePhrase is true and no wake word present`() {
        val result = detector.process("Turn on flashlight", requireWakePhrase = true)
        assertEquals(WakePhraseResult.None, result)
    }

    @Test
    fun `text normalization removes extra spaces and surrounding punctuation`() {
        val normalized = detector.normalizeText("   ... Hello   Cypher,  what is the time?   ")
        assertEquals("hello cypher, what is the time", normalized)
    }
}
