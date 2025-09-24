# LlamaEmbed - Voice Memo Architecture Documentation

## Overview

LlamaEmbed is an Android application that combines voice recording, speech recognition, and AI-powered semantic search using local embedding models. The app follows Clean Architecture principles with a focus on the voice-to-embedding pipeline for intelligent memo retrieval.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                       │
├─────────────────────────────────────────────────────────────────┤
│  VoiceMemoScreen (Compose UI) ← VoiceMemoViewModel (StateFlow)  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         DOMAIN LAYER                            │
├─────────────────────────────────────────────────────────────────┤
│  • SaveVoiceMemoUseCase (Complex: Coordinates 2 repos)          │
│  • SearchMemosUseCase (Complex: Semantic search logic)          │
│  • GenerateDemoEntriesUseCase (Complex: Orchestrates pipeline)  │
│                                                                 │
│  Simple CRUD operations go directly to repositories             │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                          DATA LAYER                             │
├─────────────────────────────────────────────────────────────────┤
│  VoiceMemoRepository ← VoiceMemoDao (Room Database)             │
│  AIRepository ← LLamaAndroid (JNI) ← llama.cpp                  │
│  SpeechRecognitionManager ← Android Speech API                  │
└─────────────────────────────────────────────────────────────────┘
```

## Voice-to-Embedding Pipeline

The core workflow transforms user voice input into searchable semantic embeddings stored in the database:

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐    ┌─────────────┐
│   USER      │    │   ANDROID    │    │   KOTLIN    │    │  DATABASE   │
│   VOICE     │───▶│   SPEECH     │──▶│    TEXT     │──▶ │   STORAGE   │
│             │    │     API      │    │             │    │ (text only) │
└─────────────┘    └──────────────┘    └─────────────┘    └─────────────┘
                                                                  │
                                                                  │
┌─────────────┐    ┌──────────────┐    ┌─────────────┐    ┌──────▼──────┐
│  DATABASE   │    │     JNI      │    │   LLAMA.CPP │    │ EMBEDDING   │
│  UPDATED    │◀───│   WRAPPER    │◀──│   LIBRARY   │◀── │ GENERATION  │
│(text+embed) │    │              │    │             │    │             │
└─────────────┘    └──────────────┘    └─────────────┘    └─────────────┘
```

### Linear Pipeline Flow

```
1. User Voice Input
         ↓
2. Android Speech Recognition API
         ↓
3. Text Result ("Buy ingredients for pizza")
         ↓
4. Save Text to Database (memo_id = 42)
         ↓
5. Generate Embedding via JNI → llama.cpp → EmbeddingGemma
         ↓
6. Update Database: memo_id 42 gets embedding BLOB
         ↓
7. Memo Ready for Semantic Search
```

### Detailed Component Flow

#### Voice Recording → Text Conversion
```
SpeechRecognitionManager.startListening()
                ↓
    Android Speech Recognition Service
                ↓
    Audio Processing & Recognition
                ↓
    "Buy ingredients for pizza tonight"
```

#### Text Storage → Embedding Generation
```
SaveVoiceMemoUseCase.execute(text, duration)
                ↓
VoiceMemoRepository.insertMemo(entity) → Returns memo_id
                ↓
AIRepository.generateEmbedding(text)
                ↓
LLamaAndroid.get_embeddings(context, text) [JNI Call]
                ↓
llama-android.cpp → llama.cpp → EmbeddingGemma Model
                ↓
Float[768] embedding array
                ↓
Convert to ByteArray
                ↓
VoiceMemoRepository.updateEmbedding(memo_id, bytes)
                ↓
Database Updated: Text + Embedding BLOB stored
```

## Key Components

### Domain Layer - Use Cases

**Architecture Principle**: Use cases are only used for complex operations that coordinate multiple repositories or contain significant business logic. Simple CRUD operations go directly from ViewModel to Repository.

#### Complex Use Cases (Justified)

##### SaveVoiceMemoUseCase
- **Purpose**: Orchestrates memo saving and embedding generation
- **Complexity**: Coordinates VoiceMemoRepository + AIRepository
- **Flow**:
  1. Save memo text to database (gets ID immediately)
  2. Initialize AI model if needed
  3. Generate embedding for text
  4. Update memo with embedding bytes
- **Key Feature**: Lazy AI model initialization for performance

##### SearchMemosUseCase
- **Purpose**: Performs semantic search across stored memos
- **Complexity**: Advanced semantic search algorithm with similarity calculations
- **Algorithm**:
  1. Generate embedding for search query
  2. Retrieve all memos with embeddings
  3. Calculate cosine similarity for each memo
  4. Filter results above threshold (0.62)
  5. Sort by timestamp (newest first)
- **Key Feature**: Comprehensive logging for similarity debugging

##### GenerateDemoEntriesUseCase
- **Purpose**: Creates 20 realistic demo entries for testing
- **Complexity**: Orchestrates multiple memo saves with progress tracking
- **Categories**: Work/Meetings, Food/Cooking, Health/Fitness, Travel, Personal, Learning
- **Flow**: Uses SaveVoiceMemoUseCase to ensure full pipeline execution

#### Simple Operations (Direct Repository Access)

The following operations bypass use cases and go directly from ViewModel to Repository:

- **Get All Memos**: `voiceMemoRepository.getAllMemos()`
- **Get Memo Count**: `voiceMemoRepository.getMemoCount()`
- **Delete Memo**: `voiceMemoRepository.deleteMemoById(id)`
- **Get Memo by ID**: `voiceMemoRepository.getMemoById(id)` (when needed)

**Rationale**: These are simple CRUD operations that don't require coordination between repositories or complex business logic.

### Data Layer - Repositories

#### VoiceMemoRepository
- **Storage**: Room database with BLOB storage for embeddings
- **Entity**: VoiceMemoEntity with text, timestamp, embedding, duration
- **Key Methods**:
  - `insertMemo()` - Save memo and return ID
  - `updateEmbedding()` - Store ByteArray embedding
  - `getMemosWithEmbeddings()` - Retrieve searchable memos

#### AIRepository
- **Purpose**: Manages LLama model and embedding operations
- **Model**: EmbeddingGemma-300M with Q4_K_M quantization
- **Key Operations**:
  - Model initialization and loading
  - Text-to-embedding conversion
  - Cosine similarity calculation
  - ByteArray serialization/deserialization

### JNI Integration

#### LLamaAndroid Class
```kotlin
external fun load_model(modelPath: String): Long
external fun get_embeddings(context: Long, text: String): FloatArray
external fun free_model(context: Long)
```

#### Native Implementation (llama-android.cpp)
```cpp
JNIEXPORT jfloatArray JNICALL
Java_android_llama_cpp_LLamaAndroid_get_1embeddings(JNIEnv *env, jobject, jlong context, jstring text) {
    // 1. Convert Java string to C++ string
    // 2. Tokenize text using llama tokenizer
    // 3. Process tokens through model
    // 4. Extract embeddings using llama_get_embeddings_seq()
    // 5. Convert to Java float array
    return result;
}
```

## Data Flow Examples

### Recording a Voice Memo
```
User presses record → SpeechRecognitionManager.startListening()
                                      ↓
                     Android Speech API processes audio
                                      ↓
                     Text result → SaveVoiceMemoUseCase.execute() [Complex UseCase]
                                      ↓
                     Save to database → Generate embedding
                                      ↓
                     Update memo with embedding → UI notification
```

### Performing Semantic Search
```
User types "food" → updateSearchQuery("food")
                                      ↓
                    SearchMemosUseCase.searchMemos() [Complex UseCase]
                                      ↓
                    Generate embedding for "food"
                                      ↓
                    Loop through all memos:
                    - Calculate similarity with "Buy ingredients for pizza"
                    - Calculate similarity with "Team meeting notes"
                    - Calculate similarity with "Meal prep planning"
                                      ↓
                    Filter results above 0.62 threshold
                                      ↓
                    Return food-related memos ranked by relevance
```

### Simple CRUD Operations
```
User deletes memo → ViewModel.deleteMemo() → voiceMemoRepository.deleteMemoById() [Direct]
User views memos → ViewModel init → voiceMemoRepository.getAllMemos() [Direct]
User sees count → ViewModel init → voiceMemoRepository.getMemoCount() [Direct]
```

### Click-to-Search Feature
```
User clicks memo → selectMemo(memo)
                             ↓
               Check if already selected
                             ↓
               If new: updateSearchQuery(memo.text)
               If same: clearSearch()
                             ↓
               Trigger semantic search with memo content
```

## Technical Specifications

### Embedding Model
- **Model**: EmbeddingGemma-300M
- **Quantization**: Q4_K_M (4-bit quantization)
- **Dimensions**: 768-dimensional vectors
- **Library**: llama.cpp with JNI wrapper
- **Performance**: Runs locally on device, no internet required

### Similarity Thresholds
- **Primary Threshold**: 0.62 (determined through testing)
- **Performance**: "guitar" → "learn guitar basics" = 0.693
- **Considerations**: Lower thresholds include more results but reduce precision

### Database Schema
```sql
CREATE TABLE voice_memos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    text TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    embedding BLOB,
    duration INTEGER NOT NULL DEFAULT 0
);
```

## Performance Considerations

### Model Loading
- **Strategy**: Lazy initialization to avoid startup delays
- **Caching**: Model stays loaded in memory once initialized
- **Fallback**: Graceful degradation if model fails to load

### Embedding Storage
- **Format**: ByteArray (BLOB) for efficient database storage
- **Size**: ~3KB per embedding (768 floats × 4 bytes)
- **Retrieval**: In-memory processing for similarity calculations

### Search Optimization
- **Algorithm**: Linear scan with cosine similarity
- **Scaling**: Suitable for hundreds of memos, consider vector DB for thousands
- **Debouncing**: 300ms delay for search input to reduce computation

## Future Enhancements

### Potential Improvements
1. **Vector Database**: Replace linear scan with approximate nearest neighbor search
2. **Model Upgrade**: Experiment with larger embedding models
3. **Batch Processing**: Generate embeddings for multiple memos simultaneously
4. **Caching**: Cache frequently accessed embeddings in memory
5. **Quantization**: Explore different quantization strategies for better accuracy

### Architecture Extensions
1. **Multi-modal Search**: Combine text embeddings with audio features
2. **Personalization**: User-specific embedding fine-tuning
3. **Export/Import**: Backup and restore embeddings across devices
4. **Real-time Sync**: Cloud synchronization with encryption

## Architecture Decisions

### Use Case Guidelines
**When to Use Use Cases:**
- Complex operations that coordinate multiple repositories
- Operations with significant business logic
- Operations that require orchestration or workflow management

**When to Skip Use Cases:**
- Simple CRUD operations (Create, Read, Update, Delete)
- Single repository calls with no business logic
- Direct data transformations

### Current Implementation
```kotlin
// Complex operations - Use Use Cases
saveVoiceMemoUseCase.execute(text, duration)        // Coordinates 2 repos
searchMemosUseCase.searchMemos(queryFlow)           // Complex algorithm
generateDemoEntriesUseCase.execute()                // Orchestrates pipeline

// Simple operations - Direct repository calls
voiceMemoRepository.getAllMemos()                   // Simple read
voiceMemoRepository.deleteMemoById(id)              // Simple delete
voiceMemoRepository.getMemoCount()                  // Simple count
```

## Development Notes

### Key Insights
- EmbeddingGemma performs well for general semantic understanding
- Threshold tuning is crucial for balancing precision vs recall
- JNI integration requires careful memory management
- Local processing ensures privacy and offline functionality
- Clean Architecture flexibility: Use cases when needed, skip when overkill

### Common Issues
- Model initialization failures require graceful fallbacks
- Similarity scores vary significantly across model types
- ByteArray serialization must handle endianness correctly
- Speech recognition accuracy affects embedding quality
- Over-architecting simple CRUD operations reduces maintainability

This architecture enables powerful local semantic search while maintaining user privacy and offline functionality through the embedded AI model approach, with a pragmatic balance between clean architecture principles and practical simplicity.