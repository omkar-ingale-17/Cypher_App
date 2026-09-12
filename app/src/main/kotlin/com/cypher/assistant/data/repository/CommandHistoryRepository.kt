package com.cypher.assistant.data.repository

import com.cypher.assistant.core.command.CommandIntent
import com.cypher.assistant.core.command.CommandResult
import com.cypher.assistant.core.common.Constants
import com.cypher.assistant.data.database.dao.CommandHistoryDao
import com.cypher.assistant.data.database.entities.CommandHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for persisting and querying command history.
 *
 * Hides the DAO from the ViewModel and use-case layers - only this class
 * knows about the database schema.
 */
@Singleton
class CommandHistoryRepository @Inject constructor(
    private val dao: CommandHistoryDao
) {
    /** Live-updating stream of the most recent commands. */
    fun observeRecentHistory(): Flow<List<CommandHistoryEntity>> =
        dao.observeRecent(Constants.MAX_COMMAND_HISTORY)

    /**
     * Persists a command->result pair and trims history to [Constants.MAX_COMMAND_HISTORY].
     * Safe to call from any coroutine context - Room handles the IO dispatch.
     */
    suspend fun record(intent: CommandIntent, result: CommandResult) {
        dao.insert(
            CommandHistoryEntity(
                rawText        = intent.rawText,
                intentType     = intent.intentType,
                source         = intent.source,
                success        = result.success,
                responseMessage = result.message
            )
        )
        dao.trimToLimit(Constants.MAX_COMMAND_HISTORY)
    }

    /** Deletes all command history. */
    suspend fun clearAll() = dao.clearAll()

    /** Returns the total number of stored commands. */
    suspend fun count(): Int = dao.count()
}
