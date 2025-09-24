package com.example.llamaembed.data.repository

import android.util.Log
import com.example.llamaembed.data.local.VoiceMemoDao
import com.example.llamaembed.data.local.VoiceMemoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for voice memo data operations
 *
 * Features:
 * - Flow-based reactive queries for real-time UI updates
 * - Database CRUD operations through DAO
 * - Proper error handling and logging
 */
@Singleton
class VoiceMemoRepository @Inject constructor(
    private val voiceMemoDao: VoiceMemoDao
) {
    companion object {
        private const val TAG = "VoiceMemoRepository"
    }




    /**
     * Get all memos as Flow for reactive UI updates
     */
    fun getAllMemos(): Flow<List<VoiceMemoEntity>> {
        return voiceMemoDao.getAllMemos()
            .catch { e ->
                Log.e(TAG, "Error fetching all memos", e)
                emit(emptyList())
            }
    }

    /**
     * Get memo by ID with Flow
     */
    fun getMemoById(id: Long): Flow<VoiceMemoEntity?> {
        return voiceMemoDao.getMemoById(id)
            .catch { e ->
                Log.e(TAG, "Error fetching memo by id: $id", e)
                emit(null)
            }
    }

    /**
     * Get total memo count as Flow
     */
    fun getMemoCount(): Flow<Int> {
        return voiceMemoDao.getMemoCount()
            .catch { e ->
                Log.e(TAG, "Error fetching memo count", e)
                emit(0)
            }
    }

    /**
     * Search memos by text content (basic text search)
     */
    fun searchMemosByText(query: String): Flow<List<VoiceMemoEntity>> {
        return voiceMemoDao.searchMemosByText(query)
            .catch { e ->
                Log.e(TAG, "Error during text search", e)
                emit(emptyList())
            }
    }

    /**
     * Get memos that have embeddings
     */
    fun getMemosWithEmbeddings(): Flow<List<VoiceMemoEntity>> {
        return voiceMemoDao.getMemosWithEmbeddings()
            .catch { e ->
                Log.e(TAG, "Error fetching memos with embeddings", e)
                emit(emptyList())
            }
    }

    /**
     * Get memos without embeddings
     */
    fun getMemosWithoutEmbeddings(): Flow<List<VoiceMemoEntity>> {
        return voiceMemoDao.getMemosWithoutEmbeddings()
            .catch { e ->
                Log.e(TAG, "Error fetching memos without embeddings", e)
                emit(emptyList())
            }
    }

    /**
     * Insert a new voice memo
     */
    suspend fun insertMemo(memo: VoiceMemoEntity): Long = withContext(Dispatchers.IO) {
        try {
            voiceMemoDao.insertMemo(memo)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting memo", e)
            throw e
        }
    }

    /**
     * Update embedding for a specific memo
     */
    suspend fun updateEmbedding(id: Long, embedding: ByteArray) = withContext(Dispatchers.IO) {
        try {
            voiceMemoDao.updateEmbedding(id, embedding)
            Log.d(TAG, "Updated embedding for memo $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating embedding for memo $id", e)
            throw e
        }
    }

    /**
     * Delete a memo by ID
     */
    suspend fun deleteMemoById(id: Long) = withContext(Dispatchers.IO) {
        try {
            voiceMemoDao.deleteMemoById(id)
            Log.d(TAG, "Deleted memo with id: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting memo: $id", e)
            throw e
        }
    }
}