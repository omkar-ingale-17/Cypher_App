package com.cypher.assistant.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandSource

/**
 * Persisted record of each command processed by Cypher.
 *
 * Used for:
 *  - Command history UI
 *  - Repeat-last-command functionality
 *  - Analytics / command frequency
 *
 * Indexed on [timestampMs] for efficient time-ordered queries.
 */
@Entity(
    tableName = "command_history",
    indices = [Index(value = ["timestampMs"])]
)
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawText: String,
    val intentType: CommandIntentType,
    val source: CommandSource,
    val success: Boolean,
    val responseMessage: String,
    val timestampMs: Long = System.currentTimeMillis()
)
