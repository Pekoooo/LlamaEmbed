# LlamaEmbed - Offline AI Voice Memos with Semantic Search Using Local Embeddings

A modern Android voice memo application that uses EmbeddingGemma model via llama.cpp for semantic search and AI-powered memo organization.

## Features

### Core Functionality
-  **Voice Recording**: Record voice memos using Android's built-in speech recognition
-  **Speech-to-Text**: Automatic transcription of voice recordings
-  **Semantic Search**: AI-powered search using embedding similarity
-  **Local Storage**: Offline-first approach with Room database
-  **Smart Organization**: Automatic memo categorization using embeddings
-  **Tap note to search**: Tap a note to semantically search similar notes in your database

## Architecture

### Tech Stack
- **UI**: Jetpack Compose with Material 3 design
- **Architecture**: MVVM with Repository pattern and Clean Architecture principles
- **Database**: Room with Flow-based reactive queries
- **ML**: llama.cpp with JNI for on-device inference
- **DI**: Hilt for clean dependency management
- **Async**: Kotlin Coroutines and Flow-based reactive programming
- **Speech**: Android SpeechRecognizer API
- **Testing**: Unit and integration tests included
- **Performance**: Optimized for mobile with efficient model loading

### Project Structure
```
app/src/main/java/com/example/llamaembed/
├── data/
│   ├── local/              # Room database entities and DAOs
│   └── repository/         # Repository implementations with Flow
├── ml/                     # Embedding result classes
├── domain/                 # Use cases for complex business logic
├── speech/                 # Speech recognition functionality
├── ui/
│   ├── components/         # Reusable Compose components
│   ├── screens/           # Main app screens
│   ├── viewmodel/         # ViewModels with StateFlow
│   └── theme/             # Material 3 theming
├── di/                    # Hilt dependency injection modules
├── MainActivity.kt        # Single activity with Compose
└── VoiceMemoApplication.kt # Application class with model initialization
```

## Key Components

### Data Layer
- **VoiceMemoEntity**: Room entity with embedding blob storage
- **VoiceMemoDao**: Flow-based database queries
- **VoiceMemoRepository**: Repository with semantic search capabilities

### ML Integration Pipeline
The ML pipeline flows through multiple layers for local AI processing:

- **Kotlin Layer (AIRepository)**: High-level embedding operations and similarity calculations
- **JNI Wrapper (LLamaAndroid.kt)**: Bridge between Kotlin and native C++ code
- **Native C++ (llama-android.cpp)**: Direct interface to llama.cpp library
- **llama.cpp Library**: Core inference engine for compatible models
- **Model Processing**: Text tokenization, embedding generation, and vector operations
- **Cosine Similarity**: Mathematical similarity scoring between embedding vectors

**Data Flow**: Text → JNI → C++ → llama.cpp → Embedding Model → Float Array → ByteArray → Database

### UI Components
- **RecordButton**: Animated FAB with state transitions
- **SearchBar**: Real-time search with debouncing
- **VoiceMemoScreen**: Main screen with comprehensive state management

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0+)
- Device with microphone support

### Model Setup
1. Download the EmbeddingGemma GGUF model files (or any other llama.cpp compatible models)
2. Place them in `app/src/main/assets/models/`:
   ```
   models/
   ├── embeddinggemma-300m.Q4_K_M.gguf
   └── [other compatible .gguf models]
   ```

**Note**: Model files are excluded from git due to size limits. You need to obtain and place them manually.

### Build and Run
1. Clone the repository
2. Open in Android Studio
3. Add model files to assets (see above)
4. Build and run on device or emulator

## Usage

### Recording Memos
1. Tap the microphone FAB to start recording
2. Speak your memo (speech recognition provides real-time feedback)
3. Tap stop or wait for automatic completion
4. Memo is automatically transcribed and saved with embedding

### Searching Memos
1. Use the search bar for natural language queries
2. Semantic search finds conceptually similar memos
3. Results are ranked by similarity score
4. Traditional text search is also supported

### Managing Memos
- **View**: Tap memo to see semantically searched similar memos
- **Delete**: Delete button on the memo
- **Refresh**: Refresh button on the top right corner of the screen
- **Generate Memos**: Tap the "+" button on the top right corner to generate 20 memos
## Flow-Based Architecture

### StateFlow in ViewModel
```kotlin
// Reactive UI state management
val uiState: StateFlow<VoiceMemoUiState> = _uiState.asStateFlow()

// One-time events
val uiEvents: Flow<VoiceMemoUiEvent> = _uiEvents.receiveAsFlow()
```

### Repository with Flows
```kotlin
// Real-time database updates
fun getAllMemos(): Flow<List<VoiceMemo>>

// Debounced search
fun searchMemos(queryFlow: Flow<String>): Flow<List<VoiceMemo>>
```

### Compose Integration
```kotlin
// Lifecycle-aware state collection
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// One-time event handling
LaunchedEffect(viewModel) {
    viewModel.uiEvents.collectLatest { event ->
        // Handle UI events
    }
}
```

## Performance Considerations

### Model Loading
- Asynchronous initialization in Application class
- Background thread for inference
- Efficient memory management

## Permissions

The app requires the following permissions:
- `RECORD_AUDIO`: For voice recording


---

