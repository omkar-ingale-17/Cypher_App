package com.cypher.assistant.core.di

import com.cypher.assistant.core.command.CommandHandler
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Core DI module - provides the empty base Set<CommandHandler> for Hilt multibinding.
 *
 * Individual feature modules add their handlers via `@Binds @IntoSet` in their
 * own Hilt modules. This ensures the router always has a valid (possibly empty)
 * set even before any feature module is added.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    /**
     * Declares the multibinding target. Hilt will inject all @IntoSet-bound
     * CommandHandler instances from all modules as a single Set<CommandHandler>.
     */
    @Multibinds
    abstract fun bindCommandHandlers(): Set<CommandHandler>
}
