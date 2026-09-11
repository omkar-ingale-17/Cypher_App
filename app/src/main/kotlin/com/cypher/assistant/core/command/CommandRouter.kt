package com.cypher.assistant.core.command

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CommandRouter"

/**
 * The central routing hub.
 *
 * Receives a [CommandIntent] from the NLU layer and delegates execution to the
 * appropriate [CommandHandler]. Handlers are registered via Hilt multibinding
 * (`@Binds @IntoSet`) — adding a new handler requires zero changes here.
 *
 * The internal dispatch map is built lazily on first use, so startup cost is zero.
 *
 * @param handlers All [CommandHandler] instances registered via Hilt @IntoSet.
 */
@Singleton
class CommandRouter @Inject constructor(
    private val handlers: Set<@JvmSuppressWildcards CommandHandler>
) {
    /**
     * Lazy map from [CommandIntentType] → [CommandHandler].
     * Built once; duplicate registrations log a warning and keep the first binding.
     */
    private val dispatchMap: Map<CommandIntentType, CommandHandler> by lazy {
        buildMap {
            handlers.forEach { handler ->
                handler.supportedIntents.forEach { intentType ->
                    if (containsKey(intentType)) {
                        Log.w(TAG, "Duplicate handler for $intentType — '${handler::class.simpleName}' ignored.")
                    } else {
                        put(intentType, handler)
                        Log.v(TAG, "Registered: $intentType → ${handler::class.simpleName}")
                    }
                }
            }
        }.also {
            Log.i(TAG, "CommandRouter ready — ${it.size} intent(s) mapped across ${handlers.size} handler(s).")
        }
    }

    /**
     * Routes [intent] to the correct handler and returns its [CommandResult].
     *
     * - If [intent.intentType] is [CommandIntentType.UNKNOWN], returns a friendly failure immediately.
     * - If no handler is registered, returns a "coming soon" failure.
     * - If the handler throws (which it should not), catches the exception and returns a failure.
     */
    suspend fun route(intent: CommandIntent): CommandResult {
        Log.d(TAG, "Routing → ${intent.intentType} (confidence=${intent.confidence}, src=${intent.source})")

        if (intent.intentType == CommandIntentType.UNKNOWN) {
            return CommandResult.failure(
                "I didn't understand \"${intent.rawText.truncate(40)}\". Could you rephrase that?"
            )
        }

        val handler = dispatchMap[intent.intentType]
            ?: return CommandResult.notYetImplemented(intent.intentType.name)

        return try {
            handler.handle(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled exception in ${handler::class.simpleName} for ${intent.intentType}", e)
            CommandResult.failure("Something went wrong. Please try again.", e)
        }
    }

    /** Returns true if at least one handler is registered for [intentType]. */
    fun canHandle(intentType: CommandIntentType): Boolean =
        dispatchMap.containsKey(intentType)

    /** Returns a sorted list of all currently supported intent types. */
    fun supportedIntents(): List<CommandIntentType> =
        dispatchMap.keys.sortedBy { it.name }

    /** Returns the number of registered intent→handler mappings. */
    fun handlerCount(): Int = dispatchMap.size

    // Helper used in the failure message above
    private fun String.truncate(max: Int) =
        if (length <= max) this else take(max - 1) + "…"
}
