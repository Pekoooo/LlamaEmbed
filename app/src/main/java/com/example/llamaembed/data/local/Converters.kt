package com.example.llamaembed.data.local

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room type converters for custom data types
 * Handles conversion between Kotlin types and SQLite-compatible types
 */
class Converters {

    /**
     * Convert timestamp from Long to Date
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Convert Date to timestamp (Long)
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}