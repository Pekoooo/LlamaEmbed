package com.example.llamaembed

import android.app.Application
import android.util.Log
import com.example.llamaembed.data.repository.AIRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VoiceMemoApplication : Application() {

    @Inject
    lateinit var aiRepository: AIRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "VoiceMemoApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // Start AI model initialization in background
        applicationScope.launch {
            try {
                Log.d(TAG, "🚀 Starting AI model initialization...")
                val success = aiRepository.initialize()
                if (success) {
                    Log.d(TAG, "✅ AI model initialized successfully during app startup")
                } else {
                    Log.w(TAG, "⚠️ AI model initialization failed during app startup")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during AI model initialization: ${e.message}", e)
            }
        }
    }
}