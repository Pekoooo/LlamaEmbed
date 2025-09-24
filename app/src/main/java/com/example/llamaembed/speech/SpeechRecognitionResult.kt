package com.example.llamaembed.speech

/**
 * Sealed class representing speech recognition results
 * Provides type-safe handling of different recognition states
 */
sealed class SpeechRecognitionResult {
    /**
     * Speech recognition completed successfully
     * @property text The transcribed text from speech
     * @property confidence Confidence score (0.0 to 1.0)
     */
    data class Success(
        val text: String,
        val confidence: Float = 1.0f
    ) : SpeechRecognitionResult()

    /**
     * Error during speech recognition
     * @property errorCode Android SpeechRecognizer error code
     * @property message Human-readable error message
     */
    data class Error(
        val errorCode: Int,
        val message: String
    ) : SpeechRecognitionResult()

    /**
     * Speech recognition is actively listening
     */
    object Listening : SpeechRecognitionResult()

    /**
     * Processing speech input (recognizing)
     */
    object Processing : SpeechRecognitionResult()

    /**
     * Speech recognition is idle/not active
     */
    object Idle : SpeechRecognitionResult()

    /**
     * Ready to start speech recognition
     */
    object Ready : SpeechRecognitionResult()
}

/**
 * Extension functions for convenient state checking
 */
fun SpeechRecognitionResult.isListening(): Boolean = this is SpeechRecognitionResult.Listening

fun SpeechRecognitionResult.isProcessing(): Boolean = this is SpeechRecognitionResult.Processing

fun SpeechRecognitionResult.isActive(): Boolean =
    this is SpeechRecognitionResult.Listening || this is SpeechRecognitionResult.Processing

fun SpeechRecognitionResult.getTextOrNull(): String? {
    return when (this) {
        is SpeechRecognitionResult.Success -> text
        else -> null
    }
}