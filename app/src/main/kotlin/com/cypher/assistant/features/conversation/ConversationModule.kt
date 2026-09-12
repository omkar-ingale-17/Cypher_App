package com.cypher.assistant.features.conversation

import com.cypher.assistant.core.command.CommandHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConversationModule {

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindConversationHandler(
        impl: ConversationHandler
    ): CommandHandler
}
