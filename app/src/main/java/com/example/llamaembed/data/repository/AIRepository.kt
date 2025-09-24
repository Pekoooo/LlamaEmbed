package com.example.llamaembed.data.repository

import android.util.Log
import com.example.llamaembed.data.ai.AIRepository as AIInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Repository for AI embedding operations
 *
 * Features:
 * - LLama model embedding generation
 * - Cosine similarity calculations
 * - ByteArray conversion utilities
 */
@Singleton
class AIRepository @Inject constructor(
    private val llamaAIRepository: AIInterface
) {
    companion object {
        private const val TAG = "AIRepository"
    }

    /**
     * Initialize AI models
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val llamaSuccess = llamaAIRepository.initialize()
            Log.d(TAG, "LLama initialized: $llamaSuccess")
            llamaSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AI model", e)
            false
        }
    }

    /**
     * Generate embeddings using LLama model
     */
    suspend fun generateEmbedding(text: String): FloatArray? = withContext(Dispatchers.Default) {
        try {
            if (llamaAIRepository.isModelLoaded) {
                Log.d(TAG, "Generating embedding with LLama model")
                return@withContext llamaAIRepository.getEmbeddings(text)
            } else {
                Log.w(TAG, "LLama model not loaded")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embedding", e)
            null
        }
    }

    /**
     * Calculate cosine similarity between two embeddings
     */
    fun calculateCosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        require(embedding1.size == embedding2.size) {
            "Embedding dimensions must match"
        }

        var dotProduct = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f

        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }

        val magnitude = sqrt(norm1 * norm2)
        return if (magnitude > 0) dotProduct / magnitude else 0.0f
    }

    /**
     * Convert embedding FloatArray to ByteArray for database storage
     */
    fun embeddingToByteArray(embedding: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        embedding.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    /**
     * Convert ByteArray from database back to FloatArray embedding
     */
    fun byteArrayToEmbedding(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val embedding = FloatArray(bytes.size / 4)
        for (i in embedding.indices) {
            embedding[i] = buffer.getFloat()
        }
        return embedding
    }

    /**
     * Check if any AI model is available
     */
    val isModelLoaded: Boolean
        get() = llamaAIRepository.isModelLoaded

    /**
     * Cleanup AI models
     */
    suspend fun cleanup() = withContext(Dispatchers.IO) {
        try {
            llamaAIRepository.cleanup()
            Log.d(TAG, "AI model cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during AI cleanup", e)
        }
    }
}