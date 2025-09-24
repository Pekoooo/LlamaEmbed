# LlamaEmbed - Voice Memo App with llama.cpp Integration

A modern Android voice memo application that uses EmbeddingGemma model via llama.cpp for semantic search and AI-powered memo organization.

## Features

### Core Functionality
- 🎤 **Voice Recording**: Record voice memos using Android's built-in speech recognition
- 📝 **Speech-to-Text**: Automatic transcription of voice recordings
- 🔍 **Semantic Search**: AI-powered search using embedding similarity
- 💾 **Local Storage**: Offline-first approach with Room database
- 🏷️ **Smart Organization**: Automatic memo categorization using embeddings

### Technical Features
- 📱 **Modern UI**: Material 3 design with Jetpack Compose
- 🏗️ **MVVM Architecture**: Clean architecture with ViewModel and StateFlow
- 🔄 **Reactive Programming**: Flow-based data access and UI updates
- 💉 **Dependency Injection**: Hilt for clean dependency management
- 🧪 **Testing**: Unit and integration tests included
- 🎯 **Performance**: Optimized for mobile with efficient model loading

## Architecture

### Tech Stack
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Database**: Room with Flow-based queries
- **ML**: llama.cpp with JNI for on-device inference
- **DI**: Hilt for dependency injection
- **Async**: Kotlin Coroutines and Flows
- **Speech**: Android SpeechRecognizer API

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

### ML Integration
- **EmbeddingGemmaManager**: ONNX model loading and inference
- **Embedding Generation**: Task-specific prompts for classification
- **Cosine Similarity**: For finding similar memos

### UI Components
- **RecordButton**: Animated FAB with state transitions
- **SearchBar**: Real-time search with debouncing
- **VoiceMemoItem**: Swipe-to-delete memo cards
- **VoiceMemoScreen**: Main screen with comprehensive state management

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0+)
- Device with microphone support

### Model Setup
1. Download the EmbeddingGemma GGUF model files
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

```bash
./gradlew assembleDebug
```

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
- **View**: Tap memo to see full content and similar memos
- **Delete**: Swipe left to delete with undo option
- **Refresh**: Pull down to refresh the memo list

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

## Testing

### Unit Tests
```bash
./gradlew test
```

### UI Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
- ViewModel business logic testing
- Repository data access testing
- Compose UI component testing
- Integration testing with Hilt

## Performance Considerations

### Model Loading
- Asynchronous initialization in Application class
- Background thread for inference
- Efficient memory management

### Database Operations
- Flow-based reactive queries
- Proper indexing for search performance
- Background thread for embedding generation

### UI Optimization
- Lazy loading in lists
- Proper Compose recomposition
- Efficient state management

## Permissions

The app requires the following permissions:
- `RECORD_AUDIO`: For voice recording
- `INTERNET`: For potential model updates (optional)
- `ACCESS_NETWORK_STATE`: For network status checking

## Dependencies

### Core Dependencies
- Compose BOM 2023.10.01
- Hilt 2.48
- Room 2.5.0
- ONNX Runtime Android 1.16.0
- Coroutines 1.7.3

### UI Dependencies
- Material 3 Compose
- Accompanist Permissions 0.32.0
- Lifecycle Compose 2.7.0

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement changes with tests
4. Submit a pull request

## License

[Add your license here]

## Acknowledgments

- Google's EmbeddingGemma model
- ONNX Runtime team
- Android Jetpack Compose team
- Material Design team

---

**Note**: This app demonstrates modern Android development practices with AI integration. The EmbeddingGemma model enables powerful semantic search capabilities while maintaining privacy through on-device processing.