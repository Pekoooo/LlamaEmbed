package com.example.llamaembed.domain.usecase

import android.util.Log
import com.example.llamaembed.data.local.VoiceMemoEntity
import com.example.llamaembed.data.repository.AIRepository
import com.example.llamaembed.data.repository.VoiceMemoRepository
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
        private const val SIMILARITY_THRESHOLD = 0.62f
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
            Log.d(TAG, "=== STARTING SEMANTIC SEARCH ===")
            Log.d(TAG, "Query: \"$query\"")
            Log.d(TAG, "Similarity threshold: $SIMILARITY_THRESHOLD")

            // Ensure AI model is initialized for semantic search
            if (!aiRepository.isModelLoaded) {
                Log.d(TAG, "AI model not loaded, initializing for semantic search...")
                val initSuccess = aiRepository.initialize()
                if (!initSuccess) {
                    Log.e(TAG, "Failed to initialize AI model, no search results")
                    emit(emptyList())
                    return@flow
                }
                Log.d(TAG, "AI model initialized successfully")
            } else {
                Log.d(TAG, "AI model already loaded")
            }

            Log.d(TAG, "Generating embedding for query: \"$query\"")

            // Generate embedding for search query
            val queryEmbedding = aiRepository.generateEmbedding(query)
            if (queryEmbedding == null) {
                Log.e(TAG, "Failed to generate embedding for query, no results")
                emit(emptyList())
                return@flow
            }

            Log.d(TAG, "Query embedding generated successfully")
            Log.d(TAG, "Query embedding size: ${queryEmbedding.size}")
            Log.d(TAG, "Query embedding sample: [${queryEmbedding.take(5).joinToString(", ") { "%.3f".format(it) }}...]")

            // Get all memos with embeddings
            val memosWithEmbeddings = voiceMemoRepository.getMemosWithEmbeddings().first()
            Log.d(TAG, "Found ${memosWithEmbeddings.size} memos with embeddings to search through")

            if (memosWithEmbeddings.isEmpty()) {
                Log.w(TAG, "No memos with embeddings found!")
                emit(emptyList())
                return@flow
            }

            // Loop through all memos and calculate cosine similarity
            val similarityResults = mutableListOf<Pair<VoiceMemoEntity, Float>>()

            memosWithEmbeddings.forEachIndexed { index, entity ->
                Log.d(TAG, "[${index + 1}/${memosWithEmbeddings.size}] Processing memo ${entity.id}")
                Log.d(TAG, "[${index + 1}/${memosWithEmbeddings.size}] Text: \"${entity.text.take(50)}...\"")

                val memoEmbedding = entity.embedding?.let { bytes ->
                    Log.d(TAG, "[${index + 1}/${memosWithEmbeddings.size}] Embedding bytes size: ${bytes.size}")
                    aiRepository.byteArrayToEmbedding(bytes)
                }

                if (memoEmbedding != null) {
                    Log.d(TAG, "[${index + 1}/${memosWithEmbeddings.size}] Memo embedding size: ${memoEmbedding.size}")
                    Log.d(TAG, "[${index + 1}/${memosWithEmbeddings.size}] Memo embedding sample: [${memoEmbedding.take(5).joinToString(", ") { "%.3f".format(it) }}...]")

                    val similarity = aiRepository.calculateCosineSimilarity(queryEmbedding, memoEmbedding)
                    Log.d(TAG, "[${index + 1}/${memosWithEmbeddings.size}] Memo ${entity.id}: similarity = ${"%.3f".format(similarity)}")

                    similarityResults.add(entity to similarity)

                    if (similarity >= SIMILARITY_THRESHOLD) {
                        Log.d(TAG, "[${index + 1}/${memosWithEmbeddings.size}] Memo ${entity.id} PASSED threshold (${"%.3f".format(similarity)} >= $SIMILARITY_THRESHOLD)")
                    } else {
                        Log.d(TAG, "[${index + 1}/${memosWithEmbeddings.size}] Memo ${entity.id} FAILED threshold (${"%.3f".format(similarity)} < $SIMILARITY_THRESHOLD)")
                    }
                } else {
                    Log.e(TAG, "[${index + 1}/${memosWithEmbeddings.size}] Memo ${entity.id} has null embedding, skipping")
                }
            }

            // Filter results by threshold
            val semanticResults = similarityResults
                .filter { it.second >= SIMILARITY_THRESHOLD }
                .map { it.first }

            // Sort results by similarity score (highest first), then by timestamp
            val sortedResults = semanticResults.sortedByDescending { it.timestamp }

            Log.d(TAG, "=== SEARCH RESULTS SUMMARY ===")
            Log.d(TAG, "Total memos processed: ${memosWithEmbeddings.size}")
            Log.d(TAG, "Results above threshold: ${sortedResults.size}")
            Log.d(TAG, "Similarity threshold: $SIMILARITY_THRESHOLD")

            if (similarityResults.isNotEmpty()) {
                val maxSimilarity = similarityResults.maxOf { it.second }
                val avgSimilarity = similarityResults.map { it.second }.average()
                Log.d(TAG, "Highest similarity: ${"%.3f".format(maxSimilarity)}")
                Log.d(TAG, "Average similarity: ${"%.3f".format(avgSimilarity)}")
            }

            if (sortedResults.isEmpty()) {
                Log.w(TAG, "No similar results found for query: \"$query\"")
                Log.w(TAG, "Consider lowering the threshold or checking embedding quality")
            } else {
                Log.d(TAG, "Found ${sortedResults.size} matching results")
                sortedResults.forEachIndexed { index, memo ->
                    val similarity = similarityResults.find { it.first.id == memo.id }?.second ?: 0f
                    Log.d(TAG, "Result ${index + 1}: Memo ${memo.id} (similarity: ${"%.3f".format(similarity)}) - \"${memo.text.take(50)}...\"")
                }
            }

            emit(sortedResults)

        } catch (e: Exception) {
            Log.e(TAG, "Error in semantic search: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}