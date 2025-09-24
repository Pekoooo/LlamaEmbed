package com.example.llamaembed.di

import android.content.Context
import com.example.llamaembed.speech.SpeechRecognitionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies
 *
 * Provides:
 * - EmbeddingGemmaManager for ONNX model operations
 * - SpeechRecognitionManager for voice input
 * - Other singleton services and managers
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {


    /**
     * Provides the SpeechRecognitionManager for voice input
     * Singleton to maintain consistent speech recognition state
     */
    @Provides
    @Singleton
    fun provideSpeechRecognitionManager(
        @ApplicationContext context: Context
    ): SpeechRecognitionManager {
        return SpeechRecognitionManager(context)
    }

}