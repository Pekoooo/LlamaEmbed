package com.example.llamaembed.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

/**
 * Room database for voice memos with embeddings
 *
 * Features:
 * - Single entity (VoiceMemoEntity) with auto-generated primary key
 * - Type converters for Date handling
 * - BLOB storage for embedding vectors
 * - Migration strategy for future schema changes
 */
@Database(
    entities = [VoiceMemoEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VoiceMemoDatabase : RoomDatabase() {

    abstract fun voiceMemoDao(): VoiceMemoDao

    companion object {
        const val DATABASE_NAME = "voice_memo_database"

        /**
         * Create database instance with proper configuration
         * - WAL mode for better performance with concurrent reads/writes
         * - Fallback to destructive migration for development (should be changed for production)
         */
        fun create(context: Context): VoiceMemoDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                VoiceMemoDatabase::class.java,
                DATABASE_NAME
            )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration() // TODO: Add proper migrations for production
                .build()
        }
    }
}