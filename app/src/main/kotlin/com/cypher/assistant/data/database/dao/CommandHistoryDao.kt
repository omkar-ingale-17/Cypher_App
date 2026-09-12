package com.cypher.assistant.data.database.dao

import androidx.room.*
import com.cypher.assistant.data.database.entities.CommandHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CommandHistoryEntity)

    /** Live-updating stream of the N most recent commands. */
    @Query("SELECT * FROM command_history ORDER BY timestampMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<CommandHistoryEntity>>

    /** One-shot read of the most recent commands (for widgets / exports). */
    @Query("SELECT * FROM command_history ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<CommandHistoryEntity>

    /** All history for a specific intent type. */
    @Query("SELECT * FROM command_history WHERE intentType = :intentType ORDER BY timestampMs DESC LIMIT :limit")
    fun observeByIntent(intentType: String, limit: Int = 20): Flow<List<CommandHistoryEntity>>

    /** Total number of persisted commands. */
    @Query("SELECT COUNT(*) FROM command_history")
    suspend fun count(): Int

    @Query("DELETE FROM command_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM command_history")
    suspend fun clearAll()

    /** Enforces max-history cap - deletes oldest entries beyond [keepCount]. */
    @Query("""
        DELETE FROM command_history 
        WHERE id NOT IN (
            SELECT id FROM command_history ORDER BY timestampMs DESC LIMIT :keepCount
        )
    """)
    suspend fun trimToLimit(keepCount: Int)
}
