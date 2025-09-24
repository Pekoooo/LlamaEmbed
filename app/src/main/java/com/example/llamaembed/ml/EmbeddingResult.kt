package com.example.llamaembed.ml

/**
 * Sealed class representing the result of embedding generation
 * Provides type-safe error handling and success states
 */
sealed class EmbeddingResult {
    /**
     * Successful embedding generation
     * @property embedding 768-dimensional float array representing the text embedding
     */
    data class Success(val embedding: FloatArray) : EmbeddingResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success

            if (!embedding.contentEquals(other.embedding)) return false

            return true
        }

        override fun hashCode(): Int {
            return embedding.contentHashCode()
        }
    }

    /**
     * Error during embedding generation
     * @property message Human-readable error description
     * @property throwable Original exception if available
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : EmbeddingResult()

    /**
     * Model is still loading or initializing
     */
    object Loading : EmbeddingResult()
}

/**
 * Extension functions for convenient result handling
 */
fun EmbeddingResult.getEmbeddingOrNull(): FloatArray? {
    return when (this) {
        is EmbeddingResult.Success -> embedding
        else -> null
    }
}

fun EmbeddingResult.isSuccess(): Boolean = this is EmbeddingResult.Success

fun EmbeddingResult.isError(): Boolean = this is EmbeddingResult.Error

fun EmbeddingResult.isLoading(): Boolean = this is EmbeddingResult.Loading