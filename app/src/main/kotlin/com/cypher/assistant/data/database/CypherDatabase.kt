package com.cypher.assistant.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cypher.assistant.data.database.dao.CommandHistoryDao
import com.cypher.assistant.data.database.entities.CommandHistoryEntity
import com.cypher.assistant.data.database.typeconverters.RoomTypeConverters

/**
 * Cypher's Room database.
 *
 * Version history:
 *  1 -> Initial schema: command_history table.
 *
 * When adding new entities or changing the schema, increment [version] and add
 * an explicit Migration object in [DatabaseModule].
 */
@Database(
    entities = [CommandHistoryEntity::class],
    version = 1,
    exportSchema = true   // Schema JSON exported to app/schemas/ for migration testing
)
@TypeConverters(RoomTypeConverters::class)
abstract class CypherDatabase : RoomDatabase() {
    abstract fun commandHistoryDao(): CommandHistoryDao
}
