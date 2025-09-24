package com.example.llamaembed.data.ai

import android.content.Context
import android.llama.cpp.LLamaAndroid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LLama implementation of AIRepository for embedding generation
 */
@Singleton
class LlamaAIRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AIRepository {

    companion object {
        private const val TAG = "LlamaAIRepository"
        private const val EMBEDDING_GEMMA_MODEL_NAME = "embeddinggemma-300m.Q4_K_M.gguf"
        private const val NOMIC_EMBED_MODEL_NAME = "nomic-embed-text-v1.5.Q4_K_M.gguf"
    }

    private val llama: LLamaAndroid = LLamaAndroid.instance()
    private val initMutex = Mutex()
    private var _isModelLoaded = false

    override val isModelLoaded: Boolean
        get() = _isModelLoaded

    override suspend fun initialize(): Boolean {
        return initMutex.withLock {
            if (_isModelLoaded) {
                Log.d(TAG, "Model already loaded")
                return@withLock true
            }

            try {
                Log.d(TAG, "Initializing LLama embedding model...")
                val modelPath = copyModelFromAssets(EMBEDDING_GEMMA_MODEL_NAME)
                llama.load(modelPath)
                _isModelLoaded = true
                Log.d(TAG, "LLama embedding model loaded successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LLama model", e)
                _isModelLoaded = false
                false
            }
        }
    }

    override suspend fun getEmbeddings(text: String): FloatArray {
        checkModelLoaded()
        return try {
            Log.d(TAG, "Generating embeddings for text: \"${text.take(50)}...\"")
            llama.getEmbeddings(text)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embeddings", e)
            throw AIException("Failed to generate embeddings: ${e.message}", e)
        }
    }

    override suspend fun cleanup() {
        initMutex.withLock {
            if (_isModelLoaded) {
                try {
                    Log.d(TAG, "Cleaning up LLama model...")
                    llama.unload()
                    _isModelLoaded = false
                    Log.d(TAG, "LLama model unloaded successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during cleanup", e)
                }
            }
        }
    }

    private suspend fun copyModelFromAssets(modelFileName: String): String {
        val modelFile = File(context.filesDir, modelFileName)

        if (!modelFile.exists()) {
            Log.d(TAG, "Copying model from assets to internal storage...")
            context.assets.open("models/$modelFileName").use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Model copied successfully")
        } else {
            Log.d(TAG, "Model already exists in internal storage")
        }

        return modelFile.absolutePath
    }

    private fun checkModelLoaded() {
        if (!_isModelLoaded) {
            throw AIException("AI model is not loaded. Call initialize() first.")
        }
    }
}

/**
 * Custom exception for AI operations
 */
class AIException(message: String, cause: Throwable? = null) : Exception(message, cause)