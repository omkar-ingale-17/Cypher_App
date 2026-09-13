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

    @Test
    fun test13_detectsWakeWordAtEnd_Cypher() {
        val word = detector.containsWakeWord("What is the time Cypher")
        assertEquals("cypher", word)

        val result = detector.process("What is the time Cypher", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeWithCommand)
        val withCmd = result as WakePhraseResult.WakeWithCommand
        assertEquals("cypher", withCmd.wakeWord)
        assertEquals("what is the time", withCmd.commandPayload)
    }

    @Test
    fun test14_detectsWakeWordAtEnd_Jaan() {
        val word = detector.containsWakeWord("Tell me a joke jaan")
        assertEquals("jaan", word)

        val result = detector.process("Tell me a joke jaan", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeWithCommand)
        val withCmd = result as WakePhraseResult.WakeWithCommand
        assertEquals("jaan", withCmd.wakeWord)
        assertEquals("tell me a joke", withCmd.commandPayload)
    }

    @Test
    fun test15_detectsWakeWordAtEnd_Baby() {
        val word = detector.containsWakeWord("Play music baby")
        assertEquals("baby", word)

        val result = detector.process("Play music baby", requireWakePhrase = true)
        assertTrue(result is WakePhraseResult.WakeWithCommand)
        val withCmd = result as WakePhraseResult.WakeWithCommand
        assertEquals("baby", withCmd.wakeWord)
        assertEquals("play music", withCmd.commandPayload)
    }

    @Test
    fun test16_extractWakeWordAndCommandDirect() {
        val res1 = detector.extractWakeWordAndCommand("cypher what is the time")
        assertEquals("cypher", res1.wakeWord)
        assertEquals("what is the time", res1.command)

        val res2 = detector.extractWakeWordAndCommand("what is the time cypher")
        assertEquals("cypher", res2.wakeWord)
        assertEquals("what is the time", res2.command)

        val res3 = detector.extractWakeWordAndCommand("cypher")
        assertEquals("cypher", res3.wakeWord)
        assertNull(res3.command)

        val res4 = detector.extractWakeWordAndCommand("random words without wake")
        assertNull(res4.wakeWord)
        assertNull(res4.command)
    }
}
