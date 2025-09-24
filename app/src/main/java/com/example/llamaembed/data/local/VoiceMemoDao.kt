package com.example.llamaembed.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object (DAO) for voice memo operations with Flow-based reactive queries
 * All queries return Flow for real-time UI updates and proper lifecycle management
 */
@Dao
interface VoiceMemoDao {

    /**
     * Get all voice memos ordered by timestamp (newest first)
     * Returns Flow for reactive UI updates
     */
    @Query("SELECT * FROM voice_memos ORDER BY timestamp DESC")
    fun getAllMemos(): Flow<List<VoiceMemoEntity>>

    /**
     * Get a specific memo by ID
     * Returns Flow for reactive updates
     */
    @Query("SELECT * FROM voice_memos WHERE id = :id")
    fun getMemoById(id: Long): Flow<VoiceMemoEntity?>

    /**
     * Search memos by text content (basic text search)
     * This is complemented by semantic search using embeddings in the repository layer
     */
    @Query("SELECT * FROM voice_memos WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMemosByText(query: String): Flow<List<VoiceMemoEntity>>

    /**
     * Get memos within a specific date range
     */
    @Query("SELECT * FROM voice_memos WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getMemosInDateRange(startDate: Date, endDate: Date): Flow<List<VoiceMemoEntity>>

    /**
     * Get count of all memos
     */
    @Query("SELECT COUNT(*) FROM voice_memos")
    fun getMemoCount(): Flow<Int>

    /**
     * Get memos that have embeddings (for semantic search)
     */
    @Query("SELECT * FROM voice_memos WHERE embedding IS NOT NULL ORDER BY timestamp DESC")
    fun getMemosWithEmbeddings(): Flow<List<VoiceMemoEntity>>

    /**
     * Get memos without embeddings (need processing)
     */
    @Query("SELECT * FROM voice_memos WHERE embedding IS NULL ORDER BY timestamp DESC")
    fun getMemosWithoutEmbeddings(): Flow<List<VoiceMemoEntity>>

    /**
     * Insert a new voice memo
     * Returns the auto-generated ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: VoiceMemoEntity): Long

    /**
     * Insert multiple memos
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemos(memos: List<VoiceMemoEntity>): List<Long>

    /**
     * Update an existing memo (useful for adding embeddings after processing)
     */
    @Update
    suspend fun updateMemo(memo: VoiceMemoEntity)

    /**
     * Update only the embedding for a specific memo
     */
    @Query("UPDATE voice_memos SET embedding = :embedding WHERE id = :id")
    suspend fun updateEmbedding(id: Long, embedding: ByteArray)

    /**
     * Delete a specific memo
     */
    @Delete
    suspend fun deleteMemo(memo: VoiceMemoEntity)

    /**
     * Delete memo by ID
     */
    @Query("DELETE FROM voice_memos WHERE id = :id")
    suspend fun deleteMemoById(id: Long)

    /**
     * Delete all memos
     */
    @Query("DELETE FROM voice_memos")
    suspend fun deleteAllMemos()

    /**
     * Delete memos older than a specific date
     */
    @Query("DELETE FROM voice_memos WHERE timestamp < :cutoffDate")
    suspend fun deleteMemosOlderThan(cutoffDate: Date)
}