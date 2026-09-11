package com.cypher.assistant.core.di

import com.cypher.assistant.core.voice.stt.AndroidSpeechRecognizerEngine
import com.cypher.assistant.core.voice.stt.SpeechRecognizerEngine
import com.cypher.assistant.core.voice.tts.AndroidTTSEngine
import com.cypher.assistant.core.voice.tts.TTSEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {

    @Binds
    @Singleton
    abstract fun bindSpeechRecognizerEngine(
        impl: AndroidSpeechRecognizerEngine
    ): SpeechRecognizerEngine

    @Binds
    @Singleton
    abstract fun bindTtsEngine(
        impl: AndroidTTSEngine
    ): TTSEngine
}
