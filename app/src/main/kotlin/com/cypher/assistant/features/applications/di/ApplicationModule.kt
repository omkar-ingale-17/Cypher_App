package com.cypher.assistant.features.applications.di

import com.cypher.assistant.core.command.CommandHandler
import com.cypher.assistant.features.applications.ApplicationCommandHandler
import com.cypher.assistant.features.applications.data.repository.ApplicationRepositoryImpl
import com.cypher.assistant.features.applications.domain.repository.ApplicationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationModule {

    @Binds
    @Singleton
    abstract fun bindApplicationRepository(
        impl: ApplicationRepositoryImpl
    ): ApplicationRepository

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindApplicationCommandHandler(
        impl: ApplicationCommandHandler
    ): CommandHandler
}
