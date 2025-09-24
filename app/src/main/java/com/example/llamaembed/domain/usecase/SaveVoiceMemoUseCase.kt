package com.example.llamaembed.domain.usecase

import android.util.Log
import com.example.llamaembed.data.local.VoiceMemoEntity
import com.example.llamaembed.data.repository.AIRepository
import com.example.llamaembed.data.repository.VoiceMemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for saving voice memos with AI-generated embeddings
 *
 * Orchestrates:
 * - Saving memo text and metadata to database
 * - Generating embeddings using AI repository
 * - Updating memo with generated embeddings
 */
@Singleton
class SaveVoiceMemoUseCase @Inject constructor(
    private val voiceMemoRepository: VoiceMemoRepository,
    private val aiRepository: AIRepository
) {

    companion object {
        private const val TAG = "VOICE_MEMO_FLOW"
    }

    /**
     * Save a voice memo with AI-generated embeddings
     *
     * @param text The transcribed text from speech recognition
     * @param duration Recording duration in milliseconds
     * @return Flow<Long> The ID of the saved memo
     */
    fun execute(text: String, duration: Long = 0): Flow<Long> = flow {
        try {
            val isDemo = duration in 30000L..120000L && text.isNotBlank()
            val entryType = if (isDemo) "DEMO ENTRY" else "VOICE RECORDING"

            Log.d(TAG, "============ VOICE MEMO SAVE FLOW STARTED ============")
            Log.d(TAG, "Entry type: $entryType")
            Log.d(TAG, "Captured text: \"$text\"")
            Log.d(TAG, "Recording duration: ${duration}ms")

            // First save the memo without embedding
            val entity = VoiceMemoEntity(
                text = text,
                timestamp = Date(),
                embedding = null,
                duration = duration
            )

            Log.d(TAG, "Saving memo to database...")
            val memoId = voiceMemoRepository.insertMemo(entity)
            emit(memoId)
            Log.d(TAG, "Memo saved to database with ID: $memoId")

            // Generate embedding using AI repository
            Log.d(TAG, "Starting AI embedding generation...")
            try {
                // Ensure AI model is initialized
                if (!aiRepository.isModelLoaded) {
                    Log.d(TAG, "Initializing AI model...")
                    val initSuccess = aiRepository.initialize()
                    if (!initSuccess) {
                        Log.w(TAG, "Failed to initialize AI model for memo $memoId")
                        return@flow
                    }
                    Log.d(TAG, "AI model initialized successfully")
                }

                val embedding = aiRepository.generateEmbedding(text)
                if (embedding != null) {
                    Log.d(TAG, "Embedding generated successfully!")
                    Log.d(TAG, "Embedding dimensions: ${embedding.size}")
                    Log.d(TAG, "First 5 values: [${embedding.take(5).joinToString(", ") { "%.4f".format(it) }}]")

                    val embeddingBytes = aiRepository.embeddingToByteArray(embedding)
                    Log.d(TAG, "Converting to ByteArray: ${embeddingBytes.size} bytes")

                    voiceMemoRepository.updateEmbedding(memoId, embeddingBytes)
                    Log.d(TAG, "Embedding saved to database for memo $memoId")
                } else {
                    Log.w(TAG, "Failed to generate embedding for memo $memoId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating embedding: ${e.message}")
            }

            Log.d(TAG, "============ VOICE MEMO SAVE FLOW COMPLETED ============")

        } catch (e: Exception) {
            Log.e(TAG, "Critical error saving memo: ${e.message}", e)
            throw e
        }
    }
}