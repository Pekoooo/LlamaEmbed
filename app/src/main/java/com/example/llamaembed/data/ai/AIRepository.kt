package com.example.llamaembed.data.ai

/**
 * Repository interface for AI embedding operations
 * Provides abstraction over embedding model implementations
 */
interface AIRepository {

    /**
     * Initialize the AI model
     * @return true if initialization was successful
     */
    suspend fun initialize(): Boolean

    /**
     * Check if the AI model is loaded and ready
     */
    val isModelLoaded: Boolean

    /**
     * Generate embeddings for the given text
     * @param text The text to generate embeddings for
     * @return FloatArray containing the embeddings
     */
    suspend fun getEmbeddings(text: String): FloatArray

    /**
     * Clean up resources and unload the model
     */
    suspend fun cleanup()
}