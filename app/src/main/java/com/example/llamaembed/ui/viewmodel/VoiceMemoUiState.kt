package com.example.llamaembed.ui.viewmodel

import com.example.llamaembed.data.local.VoiceMemoEntity
import com.example.llamaembed.speech.SpeechRecognitionResult
import com.example.llamaembed.speech.isActive

/**
 * UI state data class for the voice memo screen
 * Represents all the state needed by the Compose UI
 */
data class VoiceMemoUiState(
    // Voice memo data
    val memos: List<VoiceMemoEntity> = emptyList(),
    val totalMemoCount: Int = 0,

    // Speech recognition state
    val speechRecognitionResult: SpeechRecognitionResult = SpeechRecognitionResult.Idle,
    val isRecording: Boolean = false,

    // Search functionality
    val searchQuery: String = "",
    val searchResults: List<VoiceMemoEntity> = emptyList(),
    val isSearching: Boolean = false,

    // Loading states
    val isLoadingMemos: Boolean = false,
    val isGeneratingEmbedding: Boolean = false,
    val isGeneratingDemoEntries: Boolean = false,
    val demoGenerationProgress: Int = 0,

    // Error handling
    val errorMessage: String? = null,
    val showError: Boolean = false,

    // UI state
    val selectedMemo: VoiceMemoEntity? = null,

    // Recording duration (for UI feedback)
    val recordingDuration: Long = 0L
) {
    val isActive: Boolean
        get() = isRecording || speechRecognitionResult.isActive()

    val hasSearchQuery: Boolean
        get() = searchQuery.isNotBlank()

    val displayedMemos: List<VoiceMemoEntity>
        get() = if (hasSearchQuery) searchResults else memos

    val canStartRecording: Boolean
        get() = !isActive && !isLoadingMemos && !isGeneratingEmbedding

    val showEmptyState: Boolean
        get() = !isLoadingMemos && !hasSearchQuery && memos.isEmpty()

    val showSearchEmptyState: Boolean
        get() = !isSearching && hasSearchQuery && searchResults.isEmpty()
}

/**
 * Sealed class for one-time UI events
 * These represent actions that should happen once and not persist in state
 */
sealed class VoiceMemoUiEvent {
    data class ShowError(val message: String) : VoiceMemoUiEvent()
    data class ShowSnackbar(val message: String) : VoiceMemoUiEvent()
    data class MemoSaved(val memoId: Long) : VoiceMemoUiEvent()
    data class MemoDeleted(val memoText: String) : VoiceMemoUiEvent()
    object RecordingStarted : VoiceMemoUiEvent()
    object RecordingStopped : VoiceMemoUiEvent()
    object EmbeddingGenerated : VoiceMemoUiEvent()
}