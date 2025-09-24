package com.example.llamaembed.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for Android speech recognition with Flow-based state management
 *
 * Features:
 * - Flow-based reactive state management
 * - Automatic error handling and recovery
 * - Support for offline speech recognition when available
 * - Proper lifecycle management and cleanup
 * - Visual feedback states for UI integration
 */
@Singleton
class SpeechRecognitionManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SpeechRecognitionManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isInitialized = false

    // StateFlow for current recognition state
    private val _recognitionState = MutableStateFlow<SpeechRecognitionResult>(SpeechRecognitionResult.Idle)
    val recognitionState: StateFlow<SpeechRecognitionResult> = _recognitionState.asStateFlow()

    // Channel for one-time recognition results
    private val _recognitionResults = Channel<SpeechRecognitionResult>(Channel.UNLIMITED)
    val recognitionResults: Flow<SpeechRecognitionResult> = _recognitionResults.receiveAsFlow()

    /**
     * Initialize speech recognition components
     * Should be called when the app starts or when speech recognition is first needed
     */
    fun initialize(): Boolean {
        return try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e(TAG, "Speech recognition not available on this device")
                return false
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(recognitionListener)
            isInitialized = true
            _recognitionState.value = SpeechRecognitionResult.Ready

            Log.d(TAG, "Speech recognition initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize speech recognition", e)
            false
        }
    }

    /**
     * Start speech recognition
     * @param preferOffline Try to use offline recognition if available
     */
    fun startListening(preferOffline: Boolean = true) {
        if (!isInitialized) {
            Log.w(TAG, "Speech recognition not initialized")
            _recognitionState.value = SpeechRecognitionResult.Error(-1, "Speech recognition not initialized")
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

                // Try to prefer offline recognition
                if (preferOffline) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }

                // Additional settings for better recognition
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(Locale.US.toString()))
            }

            speechRecognizer?.startListening(intent)
            _recognitionState.value = SpeechRecognitionResult.Listening

            Log.d(TAG, "Started speech recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _recognitionState.value = SpeechRecognitionResult.Error(-1, "Failed to start recognition: ${e.message}")
        }
    }

    /**
     * Stop speech recognition
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            Log.d(TAG, "Stopped speech recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }

    /**
     * Cancel ongoing speech recognition
     */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _recognitionState.value = SpeechRecognitionResult.Idle
            Log.d(TAG, "Cancelled speech recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognition", e)
        }
    }

    /**
     * Check if speech recognition is available on this device
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Check if currently listening or processing
     */
    fun isActive(): Boolean {
        return _recognitionState.value.isActive()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isInitialized = false
            _recognitionState.value = SpeechRecognitionResult.Idle
            Log.d(TAG, "Speech recognition cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
            _recognitionState.value = SpeechRecognitionResult.Listening
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech detected")
            _recognitionState.value = SpeechRecognitionResult.Listening
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Could be used for audio level visualization
            // For now, we'll keep the current state
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received - could be used for custom processing
            Log.d(TAG, "Audio buffer received")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech detected")
            _recognitionState.value = SpeechRecognitionResult.Processing
        }

        override fun onError(error: Int) {
            val errorMessage = getErrorMessage(error)
            Log.e(TAG, "Speech recognition error: $errorMessage (code: $error)")

            val result = SpeechRecognitionResult.Error(error, errorMessage)
            _recognitionState.value = result
            _recognitionResults.trySend(result)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

            if (!matches.isNullOrEmpty()) {
                val bestMatch = matches[0]
                val confidence = confidences?.get(0) ?: 1.0f

                Log.d(TAG, "Speech recognition result: '$bestMatch' (confidence: $confidence)")

                val result = SpeechRecognitionResult.Success(bestMatch, confidence)
                _recognitionState.value = SpeechRecognitionResult.Ready
                _recognitionResults.trySend(result)
            } else {
                Log.w(TAG, "No speech recognition results")
                val result = SpeechRecognitionResult.Error(-1, "No speech detected")
                _recognitionState.value = SpeechRecognitionResult.Ready
                _recognitionResults.trySend(result)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                Log.d(TAG, "Partial result: ${matches[0]}")
                // Could emit partial results if needed for real-time feedback
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "Speech recognition event: $eventType")
        }
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech input matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language unavailable"
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "Cannot check recognition support"
            else -> "Unknown error (code: $errorCode)"
        }
    }
}