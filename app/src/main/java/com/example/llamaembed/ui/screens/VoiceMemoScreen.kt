package com.example.llamaembed.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.llamaembed.data.local.VoiceMemoEntity
import com.example.llamaembed.speech.SpeechRecognitionResult
import com.example.llamaembed.ui.components.RecordButton
import com.example.llamaembed.ui.components.SearchBar
import com.example.llamaembed.ui.components.VoiceMemoItem
import com.example.llamaembed.ui.viewmodel.VoiceMemoUiEvent
import com.example.llamaembed.ui.viewmodel.VoiceMemoUiState
import com.example.llamaembed.ui.viewmodel.VoiceMemoViewModel
import com.example.llamaembed.speech.isListening
import com.example.llamaembed.speech.isProcessing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.flow.collectLatest
import java.util.*

/**
 * Main voice memo screen with comprehensive UI and state management
 *
 * Features:
 * - Material 3 design with proper theming
 * - Permission handling for audio recording
 * - Real-time search with semantic similarity
 * - Pull-to-refresh functionality
 * - Error handling with snackbars
 * - Empty state handling
 * - Responsive layout for different screen sizes
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceMemoScreen(
    viewModel: VoiceMemoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Permission handling for audio recording
    val audioPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    // Snackbar host state for showing messages
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-time UI events
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is VoiceMemoUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long
                    )
                }
                is VoiceMemoUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is VoiceMemoUiEvent.MemoSaved -> {
                    snackbarHostState.showSnackbar(
                        message = "Memo saved successfully",
                        duration = SnackbarDuration.Short
                    )
                }
                is VoiceMemoUiEvent.MemoDeleted -> {
                    snackbarHostState.showSnackbar(
                        message = "Memo deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                }
                else -> { /* Handle other events */ }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            VoiceMemoTopBar(
                uiState = uiState,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onClearSearch = viewModel::clearSearch,
                onRefresh = viewModel::refreshMemos,
                onGenerateDemo = viewModel::generateDemoEntries,
                onDebugEmbeddings = viewModel::debugEmbeddingStatus
            )
        },
        floatingActionButton = {
            if (audioPermissionState.status.isGranted) {
                RecordButton(
                    speechState = uiState.speechRecognitionResult,
                    isGeneratingEmbedding = uiState.isGeneratingEmbedding,
                    onStartRecording = viewModel::startRecording,
                    onStopRecording = viewModel::stopRecording
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Permission not granted
                !audioPermissionState.status.isGranted -> {
                    PermissionRequestContent(
                        permissionState = audioPermissionState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Loading state
                uiState.isLoadingMemos && uiState.memos.isEmpty() -> {
                    LoadingContent(modifier = Modifier.fillMaxSize())
                }

                // Empty state (no memos)
                uiState.showEmptyState -> {
                    EmptyStateContent(
                        modifier = Modifier.fillMaxSize(),
                        onGetStarted = {
                            if (audioPermissionState.status.isGranted) {
                                viewModel.startRecording()
                            }
                        }
                    )
                }

                // Search empty state
                uiState.showSearchEmptyState -> {
                    SearchEmptyStateContent(
                        searchQuery = uiState.searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Main content
                else -> {
                    VoiceMemoList(
                        uiState = uiState,
                        onItemClick = viewModel::selectMemo,
                        onDeleteClick = viewModel::deleteMemo,
                        onRefresh = viewModel::refreshMemos,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Recording status overlay
            AnimatedVisibility(
                visible = uiState.isActive,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                RecordingStatusCard(
                    speechState = uiState.speechRecognitionResult,
                    isGeneratingEmbedding = uiState.isGeneratingEmbedding,
                    duration = uiState.recordingDuration,
                    onCancel = viewModel::cancelRecording
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceMemoTopBar(
    uiState: VoiceMemoUiState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onRefresh: () -> Unit,
    onGenerateDemo: () -> Unit,
    onDebugEmbeddings: () -> Unit
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = "Voice Memos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onDebugEmbeddings() }
                )
            },
            actions = {
                if (!uiState.hasSearchQuery && !uiState.isGeneratingDemoEntries) {
                    Text(
                        text = "${uiState.totalMemoCount} memos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                // Demo generation progress
                if (uiState.isGeneratingDemoEntries) {
                    Text(
                        text = "${uiState.demoGenerationProgress}/20",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }

                // Generate demo button
                if (!uiState.isGeneratingDemoEntries && !uiState.isActive) {
                    IconButton(onClick = onGenerateDemo) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Generate demo entries"
                        )
                    }
                }


                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh memos"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Search bar
        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearQuery = onClearSearch,
            isSearching = uiState.isSearching,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = "Search memos with AI..."
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRequestContent(
    permissionState: com.google.accompanist.permissions.PermissionState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Microphone Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (permissionState.status.shouldShowRationale) {
                "The app needs microphone access to record voice memos. Please grant the permission to continue."
            } else {
                "To record voice memos, this app needs access to your device's microphone."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { permissionState.launchPermissionRequest() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading memos...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyStateContent(
    modifier: Modifier = Modifier,
    onGetStarted: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎤",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Voice Memos Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the microphone button to record your first voice memo. Your recordings will be transcribed and made searchable using AI.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
private fun SearchEmptyStateContent(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔍",
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Results Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "No memos found for \"$searchQuery\". Try different keywords or record a new memo.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VoiceMemoList(
    uiState: VoiceMemoUiState,
    onItemClick: (VoiceMemoEntity) -> Unit,
    onDeleteClick: (VoiceMemoEntity) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = uiState.displayedMemos,
            key = { memo -> memo.id }
        ) { memo ->
            VoiceMemoItem(
                memo = memo,
                onItemClick = onItemClick,
                onDeleteClick = onDeleteClick,
                isSelected = uiState.selectedMemo?.id == memo.id,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun RecordingStatusCard(
    speechState: SpeechRecognitionResult,
    isGeneratingEmbedding: Boolean,
    duration: Long,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isGeneratingEmbedding -> "Generating embedding..."
                        speechState.isProcessing() -> "Processing speech..."
                        speechState.isListening() -> "Listening..."
                        else -> "Preparing..."
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                if (duration > 0) {
                    Text(
                        text = "${duration / 1000}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceMemoScreenPreview() {
    MaterialTheme {
        // Preview would need mock data and view model
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Voice Memo Screen Preview")
        }
    }
}