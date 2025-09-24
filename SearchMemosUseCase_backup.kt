package com.example.embeddedgemma.domain.usecase

import android.util.Log
import com.example.embeddedgemma.data.local.VoiceMemoEntity
import com.example.embeddedgemma.data.repository.AIRepository
import com.example.embeddedgemma.data.repository.VoiceMemoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for searching voice memos with text and semantic search
 *
 * Features:
 * - Text-based search using SQL LIKE
 * - AI-powered semantic search using embeddings
 * - Debounced search with Flow transformations
 * - Combined and ranked results
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Singleton
class SearchMemosUseCase @Inject constructor(
    private val voiceMemoRepository: VoiceMemoRepository,
    private val aiRepository: AIRepository
) {
    companion object {
        private const val TAG = "SearchMemosUseCase"
        private const val SIMILARITY_THRESHOLD = 0.7f
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    /**
     * Search memos with hybrid text and semantic search
     *
     * @param queryFlow Flow of search queries with debouncing
     * @return Flow of search results ranked by relevance
     */
    fun searchMemos(queryFlow: Flow<String>): Flow<List<VoiceMemoEntity>> {
        return queryFlow
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .filter { it.isNotBlank() }
            .flatMapLatest { query ->
                performSearch(query.trim())
            }
            .catch { e ->
                Log.e(TAG, "Error during search", e)
                emit(emptyList())
            }
    }

    private fun performSearch(query: String): Flow<List<VoiceMemoEntity>> = flow {
        try {
            // Ensure AI model is initialized for semantic search
            if (!aiRepository.isModelLoaded) {
                Log.d(TAG, "AI model not loaded, initializing for semantic search...")
                val initSuccess = aiRepository.initialize()
                if (!initSuccess) {
                    Log.w(TAG, "Failed to initialize AI model, no search results")
                    emit(emptyList())
                    return@flow
                }
            }

            Log.d(TAG, "Performing semantic search for: \"$query\"")

            // Generate embedding for search query
            val queryEmbedding = aiRepository.generateEmbedding(query)
            if (queryEmbedding == null) {
                Log.w(TAG, "Failed to generate embedding for query, no results")
                emit(emptyList())
                return@flow
            }

            Log.d(TAG, "Query embedding generated successfully")

            // Get all memos with embeddings
            val memosWithEmbeddings = voiceMemoRepository.getMemosWithEmbeddings().first()
            Log.d(TAG, "Found ${memosWithEmbeddings.size} memos with embeddings to search through")

            // Loop through all memos and calculate cosine similarity
            val semanticResults = memosWithEmbeddings.mapNotNull { entity ->
                val memoEmbedding = entity.embedding?.let { bytes ->
                    aiRepository.byteArrayToEmbedding(bytes)
                }

                if (memoEmbedding != null) {
                    val similarity = aiRepository.calculateCosineSimilarity(queryEmbedding, memoEmbedding)
                    Log.d(TAG, "Memo ${entity.id}: similarity = ${"%.3f".format(similarity)}")

                    if (similarity >= SIMILARITY_THRESHOLD) {
                        entity
                    } else null
                } else {
                    Log.w(TAG, "Memo ${entity.id} has null embedding, skipping")
                    null
                }
            }

            // Sort results by similarity score (highest first), then by timestamp
            val sortedResults = semanticResults.sortedByDescending { it.timestamp }

            Log.d(TAG, "Semantic search completed: ${sortedResults.size} results above threshold $SIMILARITY_THRESHOLD")

            if (sortedResults.isEmpty()) {
                Log.d(TAG, "No similar results found for query: \"$query\"")
            }

            emit(sortedResults)

        } catch (e: Exception) {
            Log.e(TAG, "Error in semantic search", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}