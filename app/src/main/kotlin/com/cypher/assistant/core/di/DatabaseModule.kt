package com.cypher.assistant.core.di

import android.content.Context
import androidx.room.Room
import com.cypher.assistant.core.common.Constants
import com.cypher.assistant.data.database.CypherDatabase
import com.cypher.assistant.data.database.dao.CommandHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCypherDatabase(@ApplicationContext context: Context): CypherDatabase =
        Room.databaseBuilder(
            context,
            CypherDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // Replace with proper migrations before production
            .build()

    @Provides
    fun provideCommandHistoryDao(db: CypherDatabase): CommandHistoryDao =
        db.commandHistoryDao()
}
