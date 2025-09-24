package com.example.llamaembed.di

import com.example.llamaembed.data.ai.AIRepository as LlamaInterface
import com.example.llamaembed.data.ai.LlamaAIRepository
import com.example.llamaembed.data.local.VoiceMemoDao
import com.example.llamaembed.data.repository.AIRepository
import com.example.llamaembed.data.repository.VoiceMemoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository layer dependencies
 *
 * Provides:
 * - Repository implementations with proper dependency injection
 * - Ensures single instance of repositories for consistent state
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    companion object {
        /**
         * Provides the VoiceMemoRepository with its dependencies
         * Singleton to ensure consistent data state across the app
         */
        @Provides
        @Singleton
        fun provideVoiceMemoRepository(
            voiceMemoDao: VoiceMemoDao
        ): VoiceMemoRepository {
            return VoiceMemoRepository(voiceMemoDao)
        }

        /**
         * Provides the AIRepository with its dependencies
         * Singleton to ensure consistent AI model state across the app
         */
        @Provides
        @Singleton
        fun provideAIRepository(
            llamaAIRepository: LlamaInterface
        ): AIRepository {
            return AIRepository(llamaAIRepository)
        }
    }

    /**
     * Binds the LlamaAIRepository implementation to LlamaInterface interface
     */
    @Binds
    @Singleton
    abstract fun bindLlamaAIRepository(
        llamaAIRepository: LlamaAIRepository
    ): LlamaInterface
}