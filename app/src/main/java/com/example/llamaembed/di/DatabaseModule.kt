package com.example.llamaembed.di

import android.content.Context
import androidx.room.Room
import com.example.llamaembed.data.local.VoiceMemoDao
import com.example.llamaembed.data.local.VoiceMemoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 *
 * Provides:
 * - Room database instance with proper configuration
 * - DAO instances for data access
 * - Singleton scope for efficient resource usage
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the Room database instance
     * Configured with proper settings for production use
     */
    @Provides
    @Singleton
    fun provideVoiceMemoDatabase(
        @ApplicationContext context: Context
    ): VoiceMemoDatabase {
        return VoiceMemoDatabase.create(context)
    }

    /**
     * Provides the VoiceMemoDao for database operations
     */
    @Provides
    fun provideVoiceMemoDao(
        database: VoiceMemoDatabase
    ): VoiceMemoDao {
        return database.voiceMemoDao()
    }
}