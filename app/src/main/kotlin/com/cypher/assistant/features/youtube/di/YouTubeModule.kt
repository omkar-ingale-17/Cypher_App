package com.cypher.assistant.features.youtube.di

import com.cypher.assistant.core.command.CommandHandler
import com.cypher.assistant.features.youtube.YouTubeCommandHandler
import com.cypher.assistant.features.youtube.data.YouTubeRepositoryImpl
import com.cypher.assistant.features.youtube.domain.repository.YouTubeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class YouTubeModule {

    @Binds
    @Singleton
    abstract fun bindYouTubeRepository(
        impl: YouTubeRepositoryImpl
    ): YouTubeRepository

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindYouTubeCommandHandler(
        impl: YouTubeCommandHandler
    ): CommandHandler
}
