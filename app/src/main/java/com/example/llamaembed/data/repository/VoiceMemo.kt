package com.example.llamaembed.data.repository

import java.util.Date

/**
 * Domain model for voice memo
 * This is the model used throughout the app (UI and business logic)
 * Separate from the database entity for clean architecture
 */
data class VoiceMemo(
    val id: Long = 0,
    val text: String,
    val timestamp: Date,
    val embedding: FloatArray? = null,
    val duration: Long = 0,
    val similarity: Float? = null // Used for search results ranking
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoiceMemo

        if (id != other.id) return false
        if (text != other.text) return false
        if (timestamp != other.timestamp) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (duration != other.duration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + duration.hashCode()
        return result
    }
}