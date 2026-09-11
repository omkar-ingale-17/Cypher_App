package com.cypher.assistant.core.command

/**
 * Contract for every feature handler in the Command Router.
 *
 * ## How to register a new handler
 * 1. Create a class implementing [CommandHandler] (e.g., `AppHandler`).
 * 2. In its Hilt DI module, add:
 *    ```kotlin
 *    @Binds @IntoSet
 *    abstract fun bindAppHandler(impl: AppHandler): CommandHandler
 *    ```
 * 3. The [CommandRouter] will automatically pick it up — no changes needed there.
 *
 * ## Threading
 * [handle] is a suspend function. Implementations should:
 *  - Use `withContext(Dispatchers.IO)` for file/network/ContentResolver work.
 *  - Use `withContext(Dispatchers.Main)` for any UI interaction (e.g., launching Activities).
 */
interface CommandHandler {

    /**
     * The set of [CommandIntentType]s this handler can process.
     * Must be non-empty. Duplicate registrations log a warning and keep the first handler.
     */
    val supportedIntents: Set<CommandIntentType>

    /**
     * Execute [intent] and return a [CommandResult] whose [CommandResult.message]
     * will be read aloud by the TTS engine.
     *
     * This function must never throw — catch all exceptions internally and return
     * [CommandResult.failure] instead.
     */
    suspend fun handle(intent: CommandIntent): CommandResult
}
