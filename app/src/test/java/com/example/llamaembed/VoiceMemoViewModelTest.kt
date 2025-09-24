package com.example.llamaembed

import com.example.llamaembed.data.repository.VoiceMemo
import com.example.llamaembed.data.repository.VoiceMemoRepository
import com.example.llamaembed.speech.SpeechRecognitionManager
import com.example.llamaembed.speech.SpeechRecognitionResult
import com.example.llamaembed.ui.viewmodel.VoiceMemoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.*

/**
 * Unit tests for VoiceMemoViewModel
 * Tests the core business logic and state management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceMemoViewModelTest {

    @Mock
    private lateinit var repository: VoiceMemoRepository

    @Mock
    private lateinit var speechRecognitionManager: SpeechRecognitionManager

    private lateinit var viewModel: VoiceMemoViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup default mock behavior
        whenever(repository.getAllMemos()).thenReturn(flowOf(emptyList()))
        whenever(repository.getMemoCount()).thenReturn(flowOf(0))
        whenever(speechRecognitionManager.initialize()).thenReturn(true)
        whenever(speechRecognitionManager.recognitionState).thenReturn(
            kotlinx.coroutines.flow.MutableStateFlow(SpeechRecognitionResult.Idle)
        )
        whenever(speechRecognitionManager.recognitionResults).thenReturn(flowOf())

        viewModel = VoiceMemoViewModel(repository, speechRecognitionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val uiState = viewModel.uiState.value

        assert(uiState.memos.isEmpty())
        assert(uiState.totalMemoCount == 0)
        assert(uiState.searchQuery.isEmpty())
        assert(!uiState.isRecording)
        assert(!uiState.showError)
    }

    @Test
    fun `search query update should trigger search`() = runTest {
        val searchQuery = "test query"
        val searchResults = listOf(
            VoiceMemo(
                id = 1,
                text = "test memo",
                timestamp = Date()
            )
        )

        whenever(repository.searchMemos(kotlinx.coroutines.flow.any())).thenReturn(
            flowOf(searchResults)
        )

        viewModel.updateSearchQuery(searchQuery)
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assert(uiState.searchQuery == searchQuery)
        assert(uiState.isSearching)
    }

    @Test
    fun `clear search should reset search state`() = runTest {
        viewModel.updateSearchQuery("test")
        viewModel.clearSearch()

        val uiState = viewModel.uiState.value
        assert(uiState.searchQuery.isEmpty())
        assert(uiState.searchResults.isEmpty())
        assert(!uiState.isSearching)
    }
}