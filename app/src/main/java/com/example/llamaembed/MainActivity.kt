package com.example.llamaembed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.llamaembed.ui.screens.VoiceMemoScreen
import com.example.llamaembed.ui.theme.EmbeddedGemmaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Voice Memo app
 *
 * Features:
 * - Hilt dependency injection
 * - Material 3 design system
 * - Edge-to-edge display support
 * - Single activity architecture with Compose
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EmbeddedGemmaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceMemoApp()
                }
            }
        }
    }
}

/**
 * Main app composable that sets up the voice memo functionality
 */
@Composable
fun VoiceMemoApp() {
    VoiceMemoScreen()
}