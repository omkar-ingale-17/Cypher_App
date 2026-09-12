package com.cypher.assistant.data.database.typeconverters

import androidx.room.TypeConverter
import com.cypher.assistant.core.command.CommandIntentType
import com.cypher.assistant.core.command.CommandSource

/**
 * Room type converters for enum fields in database entities.
 * Stores enums as their string name - safe across renames only if DB migration is handled.
 */
class RoomTypeConverters {

    @TypeConverter fun intentTypeToString(value: CommandIntentType): String = value.name
    @TypeConverter fun stringToIntentType(value: String): CommandIntentType =
        CommandIntentType.valueOf(value)

    @TypeConverter fun sourceToString(value: CommandSource): String = value.name
    @TypeConverter fun stringToSource(value: String): CommandSource =
        CommandSource.valueOf(value)
}
