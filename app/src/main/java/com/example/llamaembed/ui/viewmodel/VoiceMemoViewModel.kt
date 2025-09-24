package com.example.llamaembed.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llamaembed.data.local.VoiceMemoEntity
import com.example.llamaembed.data.repository.VoiceMemoRepository
import com.example.llamaembed.domain.usecase.GenerateDemoEntriesUseCase
import com.example.llamaembed.domain.usecase.SaveVoiceMemoUseCase
import com.example.llamaembed.domain.usecase.SearchMemosUseCase
import com.example.llamaembed.speech.SpeechRecognitionManager
import com.example.llamaembed.speech.SpeechRecognitionResult
import com.example.llamaembed.speech.isListening
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * ViewModel for voice memo functionality with comprehensive StateFlow management
 *
 * Features:
 * - StateFlow for reactive UI state management
 * - Flow-based search with debouncing
 * - Speech recognition integration
 * - Error handling with user feedback
 * - Semantic search using embeddings
 * - One-time UI events using Channel/Flow
 */
@HiltViewModel
class VoiceMemoViewModel @Inject constructor(
    private val saveVoiceMemoUseCase: SaveVoiceMemoUseCase,
    private val searchMemosUseCase: SearchMemosUseCase,
    private val generateDemoEntriesUseCase: GenerateDemoEntriesUseCase,
    private val speechRecognitionManager: SpeechRecognitionManager,
    private val voiceMemoRepository: VoiceMemoRepository
) : ViewModel() {

    companion object {
        private const val TAG = "VoiceMemoViewModel"
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    // Private mutable state
    private val _uiState = MutableStateFlow(VoiceMemoUiState())
    val uiState: StateFlow<VoiceMemoUiState> = _uiState.asStateFlow()

    // One-time UI events
    private val _uiEvents = Channel<VoiceMemoUiEvent>(Channel.UNLIMITED)
    val uiEvents: Flow<VoiceMemoUiEvent> = _uiEvents.receiveAsFlow()

    // Search query flow for debouncing
    private val _searchQuery = MutableStateFlow("")
    private val searchQueryFlow = _searchQuery.asStateFlow()

    // Recording duration tracking
    private var recordingStartTime: Long = 0L

    init {
        initializeViewModel()
    }

    private fun initializeViewModel() {
        // Initialize speech recognition
        if (!speechRecognitionManager.initialize()) {
            updateErrorState("Speech recognition not available on this device")
        }

        // Collect all memos
        viewModelScope.launch {
            voiceMemoRepository.getAllMemos()
                .catch { e ->
                    Log.e(TAG, "Error loading memos", e)
                    updateErrorState("Failed to load memos: ${e.message}")
                }
                .collect { memos ->
                    _uiState.update { it.copy(memos = memos, isLoadingMemos = false) }
                }
        }

        // Collect memo count
        viewModelScope.launch {
            voiceMemoRepository.getMemoCount()
                .catch { e ->
                    Log.e(TAG, "Error loading memo count", e)
                }
                .collect { count ->
                    _uiState.update { it.copy(totalMemoCount = count) }
                }
        }

        // Set up search functionality with debouncing
        viewModelScope.launch {
            searchMemosUseCase.searchMemos(searchQueryFlow)
                .catch { e ->
                    Log.e(TAG, "Error during search", e)
                    updateErrorState("Search failed: ${e.message}")
                }
                .collect { results ->
                    _uiState.update {
                        it.copy(
                            searchResults = results,
                            isSearching = false
                        )
                    }
                }
        }

        // Collect speech recognition state
        viewModelScope.launch {
            speechRecognitionManager.recognitionState
                .collect { state ->
                    _uiState.update {
                        it.copy(
                            speechRecognitionResult = state,
                            isRecording = state.isListening()
                        )
                    }
                }
        }

        // Collect speech recognition results
        viewModelScope.launch {
            speechRecognitionManager.recognitionResults
                .collect { result ->
                    handleSpeechRecognitionResult(result)
                }
        }
    }

    /**
     * Start voice recording
     */
    fun startRecording() {
        if (!_uiState.value.canStartRecording) {
            Log.w(TAG, "Cannot start recording in current state")
            return
        }

        try {
            recordingStartTime = System.currentTimeMillis()
            speechRecognitionManager.startListening()
            _uiEvents.trySend(VoiceMemoUiEvent.RecordingStarted)
            Log.d(TAG, "Started recording")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            updateErrorState("Failed to start recording: ${e.message}")
        }
    }

    /**
     * Stop voice recording
     */
    fun stopRecording() {
        try {
            speechRecognitionManager.stopListening()
            _uiEvents.trySend(VoiceMemoUiEvent.RecordingStopped)
            Log.d(TAG, "Stopped recording")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            updateErrorState("Failed to stop recording: ${e.message}")
        }
    }

    /**
     * Cancel ongoing recording
     */
    fun cancelRecording() {
        try {
            speechRecognitionManager.cancel()
            recordingStartTime = 0L
            _uiState.update { it.copy(recordingDuration = 0L) }
            Log.d(TAG, "Cancelled recording")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling recording", e)
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update {
            it.copy(
                searchQuery = query,
                isSearching = query.isNotBlank()
            )
        }
    }

    /**
     * Clear search query and results
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false
            )
        }
    }

    /**
     * Delete a memo
     */
    fun deleteMemo(memo: VoiceMemoEntity) {
        viewModelScope.launch {
            try {
                voiceMemoRepository.deleteMemoById(memo.id)
                _uiEvents.trySend(VoiceMemoUiEvent.MemoDeleted(memo.text.take(50)))
                Log.d(TAG, "Deleted memo: ${memo.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting memo", e)
                updateErrorState("Failed to delete memo: ${e.message}")
            }
        }
    }


    /**
     * Select a memo and perform semantic search with its content
     * If the same memo is clicked again, deselect it and show all memos
     */
    fun selectMemo(memo: VoiceMemoEntity) {
        val currentSelected = _uiState.value.selectedMemo

        if (currentSelected?.id == memo.id) {
            // Deselect memo and clear search
            _uiState.update { it.copy(selectedMemo = null) }
            clearSearch()
            Log.d(TAG, "Deselected memo ${memo.id} and cleared search")
        } else {
            // Select memo and search with its content
            _uiState.update { it.copy(selectedMemo = memo) }
            updateSearchQuery(memo.text)
            Log.d(TAG, "Selected memo ${memo.id} and searching with its content")
        }
    }

    /**
     * Clear selected memo
     */
    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedMemo = null
            )
        }
    }

    /**
     * Dismiss error message
     */
    fun dismissError() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                showError = false
            )
        }
    }

    /**
     * Refresh memos (pull-to-refresh)
     */
    fun refreshMemos() {
        _uiState.update { it.copy(isLoadingMemos = true) }
        // The flow collection will automatically update the UI when new data arrives
    }

    /**
     * Generate 20 demo entries for testing semantic search
     */
    fun generateDemoEntries() {
        if (_uiState.value.isGeneratingDemoEntries) {
            Log.w(TAG, "Demo generation already in progress")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "=== STARTING DEMO GENERATION ===")
                _uiState.update {
                    it.copy(
                        isGeneratingDemoEntries = true,
                        demoGenerationProgress = 0
                    )
                }

                generateDemoEntriesUseCase.execute()
                    .catch { e ->
                        Log.e(TAG, "Error in demo generation flow: ${e.message}", e)
                        updateErrorState("Failed to generate demo entries: ${e.message}")
                        _uiState.update {
                            it.copy(
                                isGeneratingDemoEntries = false,
                                demoGenerationProgress = 0
                            )
                        }
                    }
                    .collect { progress ->
                        Log.d(TAG, "Demo progress update: $progress/20")
                        _uiState.update { it.copy(demoGenerationProgress = progress) }

                        if (progress >= 20) {
                            _uiState.update {
                                it.copy(
                                    isGeneratingDemoEntries = false,
                                    demoGenerationProgress = 0
                                )
                            }
                            _uiEvents.trySend(VoiceMemoUiEvent.ShowSnackbar("20 demo entries generated successfully!"))
                            Log.d(TAG, "Demo entries generation completed successfully!")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Critical error in demo generation: ${e.message}", e)
                updateErrorState("Failed to generate demo entries: ${e.message}")
                _uiState.update {
                    it.copy(
                        isGeneratingDemoEntries = false,
                        demoGenerationProgress = 0
                    )
                }
            }
        }
    }

    /**
     * Get recording duration for UI feedback
     */
    fun getRecordingDuration(): Long {
        return if (recordingStartTime > 0) {
            System.currentTimeMillis() - recordingStartTime
        } else 0L
    }

    /**
     * Debug function to check embedding status of all memos
     */
    fun debugEmbeddingStatus() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== DEBUGGING EMBEDDING STATUS ===")

                // Get all memos
                val allMemos = voiceMemoRepository.getAllMemos().first()
                Log.d(TAG, "Total memos in database: ${allMemos.size}")

                // Get memos with embeddings
                val memosWithEmbeddings = voiceMemoRepository.getMemosWithEmbeddings().first()
                Log.d(TAG, "Memos with embeddings: ${memosWithEmbeddings.size}")

                // Get memos without embeddings
                val memosWithoutEmbeddings = voiceMemoRepository.getMemosWithoutEmbeddings().first()
                Log.d(TAG, "Memos without embeddings: ${memosWithoutEmbeddings.size}")

                Log.d(TAG, "=== DETAILED MEMO BREAKDOWN ===")
                allMemos.forEachIndexed { index, memo ->
                    val hasEmbedding = memo.embedding != null
                    val embeddingSize = memo.embedding?.size ?: 0
                    Log.d(TAG, "Memo ${memo.id}: \"${memo.text.take(50)}...\"")
                    Log.d(TAG, "  Has embedding: $hasEmbedding (${embeddingSize} bytes)")
                    Log.d(TAG, "  Timestamp: ${memo.timestamp}")
                    Log.d(TAG, "  Duration: ${memo.duration}ms")
                    Log.d(TAG, "  ---")
                }

                // Check for food-related entries specifically
                Log.d(TAG, "=== FOOD-RELATED ENTRIES ===")
                val foodKeywords = listOf("food", "pizza", "apple", "ingredient", "cooking", "meal")
                allMemos.filter { memo ->
                    foodKeywords.any { keyword -> memo.text.lowercase().contains(keyword.lowercase()) }
                }.forEach { memo ->
                    val hasEmbedding = memo.embedding != null
                    val embeddingSize = memo.embedding?.size ?: 0
                    Log.d(TAG, "Food memo ${memo.id}: \"${memo.text}\"")
                    Log.d(TAG, "  Has embedding: $hasEmbedding (${embeddingSize} bytes)")
                }

                _uiEvents.trySend(VoiceMemoUiEvent.ShowSnackbar("Embedding debug info logged - check console"))

            } catch (e: Exception) {
                Log.e(TAG, "Error debugging embeddings: ${e.message}", e)
            }
        }
    }

    // Private helper methods

    private fun handleSpeechRecognitionResult(result: SpeechRecognitionResult) {
        when (result) {
            is SpeechRecognitionResult.Success -> {
                val duration = getRecordingDuration()
                recordingStartTime = 0L

                saveMemo(result.text, duration)
                Log.d(TAG, "Speech recognition successful: ${result.text}")
            }

            is SpeechRecognitionResult.Error -> {
                recordingStartTime = 0L
                _uiState.update { it.copy(recordingDuration = 0L) }

                val errorMessage = "Speech recognition failed: ${result.message}"
                updateErrorState(errorMessage)
                Log.e(TAG, errorMessage)
            }

            is SpeechRecognitionResult.Listening -> {
                // Update recording duration periodically
                viewModelScope.launch {
                    while (_uiState.value.isRecording) {
                        _uiState.update { it.copy(recordingDuration = getRecordingDuration()) }
                        kotlinx.coroutines.delay(100) // Update every 100ms
                    }
                }
            }

            else -> {
                // Handle other states if needed
            }
        }
    }

    private fun saveMemo(text: String, duration: Long) {
        if (text.isBlank()) {
            updateErrorState("Cannot save empty memo")
            return
        }

        _uiState.update { it.copy(isGeneratingEmbedding = true) }

        viewModelScope.launch {
            saveVoiceMemoUseCase.execute(text, duration)
                .catch { e ->
                    Log.e(TAG, "Error saving memo", e)
                    updateErrorState("Failed to save memo: ${e.message}")
                    _uiState.update { it.copy(isGeneratingEmbedding = false) }
                }
                .collect { memoId ->
                    _uiState.update { it.copy(isGeneratingEmbedding = false) }
                    _uiEvents.trySend(VoiceMemoUiEvent.MemoSaved(memoId))
                    _uiEvents.trySend(VoiceMemoUiEvent.ShowSnackbar("Memo saved successfully"))
                    Log.d(TAG, "Memo saved with ID: $memoId")
                }
        }
    }

    private fun updateErrorState(message: String) {
        _uiState.update {
            it.copy(
                errorMessage = message,
                showError = true
            )
        }
        _uiEvents.trySend(VoiceMemoUiEvent.ShowError(message))
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionManager.cleanup()
        Log.d(TAG, "ViewModel cleared")
    }
}