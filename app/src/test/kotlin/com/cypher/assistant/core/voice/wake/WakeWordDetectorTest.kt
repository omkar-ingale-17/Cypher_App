package com.cypher.assistant.core.voice.wake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun test1_detectsExactCypher() {
        val word = detector.containsWakeWord("Cypher")
        assertEquals("cypher", word)

        val result = detector.process("Cypher", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeOnly)
        assertEquals("cypher", (result as WakePhraseResult.WakeOnly).wakeWord)
    }

    @Test
    fun test2_detectsHeyCypher() {
        val word = detector.containsWakeWord("Hey Cypher")
        assertEquals("cypher", word)

        val result = detector.process("Hey Cypher", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeOnly)
        assertEquals("cypher", (result as WakePhraseResult.WakeOnly).wakeWord)
    }

    @Test
    fun test3_detectsJaan() {
        val word = detector.containsWakeWord("Jaan")
        assertEquals("jaan", word)

        val result = detector.process("Jaan", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeOnly)
        assertEquals("jaan", (result as WakePhraseResult.WakeOnly).wakeWord)
    }

    @Test
    fun test4_detectsJan() {
        val word = detector.containsWakeWord("Jan")
        assertEquals("jaan", word)

        val result = detector.process("Jan", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeOnly)
        assertEquals("jaan", (result as WakePhraseResult.WakeOnly).wakeWord)
    }

    @Test
    fun test5_detectsBaby() {
        val word = detector.containsWakeWord("Baby")
        assertEquals("baby", word)

        val result = detector.process("Baby", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeOnly)
        assertEquals("baby", (result as WakePhraseResult.WakeOnly).wakeWord)
    }

    @Test
    fun test6_detectsCypherWithAttachedCommand() {
        val word = detector.containsWakeWord("Cypher what is the time")
        assertEquals("cypher", word)

        val result = detector.process("Cypher what is the time", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeWithCommand)
        val withCmd = result as WakePhraseResult.WakeWithCommand
        assertEquals("cypher", withCmd.wakeWord)
        assertEquals("what is the time", withCmd.commandPayload)
    }

    @Test
    fun test7_detectsCipherSpellingVariation() {
        val word = detector.containsWakeWord("cipher")
        assertEquals("cypher", word)

        val result = detector.process("cipher open settings", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeWithCommand)
        val withCmd = result as WakePhraseResult.WakeWithCommand
        assertEquals("cypher", withCmd.wakeWord)
        assertEquals("open settings", withCmd.commandPayload)
    }

    @Test
    fun test8_detectsHeyBabyCompoundCommand() {
        val word = detector.containsWakeWord("Hey Baby, play music on YouTube")
        assertEquals("baby", word)

        val result = detector.process("Hey Baby, play music on YouTube", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeWithCommand)
        val withCmd = result as WakePhraseResult.WakeWithCommand
        assertEquals("baby", withCmd.wakeWord)
        assertEquals("play music on youtube", withCmd.commandPayload)
    }

    @Test
    fun test9_detectsHelloJaan() {
        val word = detector.containsWakeWord("Hello Jaan")
        assertEquals("jaan", word)

        val result = detector.process("Hello Jaan, how are you", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeWithCommand)
        val withCmd = result as WakePhraseResult.WakeWithCommand
        assertEquals("jaan", withCmd.wakeWord)
        assertEquals("how are you", withCmd.commandPayload)
    }

    @Test
    fun test10_directCommandInManualMode() {
        val result = detector.process("turn on flashlight", requireWakePhrase = false)
        assertTrue(result is WakePhraseResult.DirectCommand)
        assertEquals("turn on flashlight", (result as WakePhraseResult.DirectCommand).commandPayload)
    }

    @Test
    fun test11_ignoresUnrelatedSpeechInWakePhraseMode() {
        val word = detector.containsWakeWord("today is a sunny day in the park")
        assertNull(word)

        val result = detector.process("today is a sunny day in the park", requireWakePhrase = true)
        assertEquals(WakePhraseResult.None, result)
    }

    @Test
    fun test12_handlesEmptyOrPunctuationOnlyInput() {
        val word = detector.containsWakeWord("   ... ,,, !!!  ")
        assertNull(word)

        val result = detector.process("   ", requireWakePhrase = true)
        assertEquals(WakePhraseResult.None, result)
    }
}
