package com.google.refereeschedule.di

import android.content.Context
import androidx.room.Room
import com.google.refereeschedule.data.local.RefereeDatabase
import com.google.refereeschedule.data.local.dao.AssignmentDao
import com.google.refereeschedule.data.local.dao.GameDao
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
    fun provideDatabase(@ApplicationContext context: Context): RefereeDatabase {
        return Room.databaseBuilder(
            context,
            RefereeDatabase::class.java,
            "referee_database"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideGameDao(database: RefereeDatabase): GameDao {
        return database.gameDao()
    }

    @Provides
    fun provideAssignmentDao(database: RefereeDatabase): AssignmentDao {
        return database.assignmentDao()
    }
}
