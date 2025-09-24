package com.example.llamaembed.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.llamaembed.speech.SpeechRecognitionResult
import com.example.llamaembed.speech.isListening
import com.example.llamaembed.speech.isProcessing
import com.example.llamaembed.speech.isActive

/**
 * Animated record button with visual feedback for different recording states
 *
 * Features:
 * - Smooth state transitions with animations
 * - Pulsing animation during recording
 * - Color changes based on recording state
 * - Proper accessibility support
 * - Material 3 design
 */
@Composable
fun RecordButton(
    speechState: SpeechRecognitionResult,
    isGeneratingEmbedding: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = speechState.isListening()
    val isProcessing = speechState.isProcessing() || isGeneratingEmbedding
    val isActive = speechState.isActive() || isGeneratingEmbedding

    // Animation for pulsing effect during recording
    val infiniteTransition = rememberInfiniteTransition(label = "record_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Color animation based on state
    val buttonColor by animateColorAsState(
        targetValue = when {
            isRecording -> MaterialTheme.colorScheme.error
            isProcessing -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(300),
        label = "button_color"
    )

    // Scale animation for visual feedback
    val scale by animateFloatAsState(
        targetValue = if (isRecording) pulseScale else 1f,
        animationSpec = if (isRecording) {
            spring(Spring.DampingRatioMediumBouncy)
        } else {
            tween(200)
        },
        label = "button_scale"
    )

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring for recording state
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulseScale)
                    .background(
                        color = buttonColor.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }

        // Main button
        FloatingActionButton(
            onClick = {
                when {
                    isRecording -> onStopRecording()
                    !isActive -> onStartRecording()
                    else -> { /* Do nothing while processing */ }
                }
            },
            modifier = Modifier
                .size(80.dp)
                .scale(scale),
            containerColor = buttonColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = if (isRecording) 8.dp else 6.dp,
                pressedElevation = if (isRecording) 12.dp else 8.dp
            )
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = when {
                        isRecording -> "Stop recording"
                        isProcessing -> "Processing..."
                        else -> "Start recording"
                    },
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordButtonPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RecordButton(
                speechState = SpeechRecognitionResult.Idle,
                isGeneratingEmbedding = false,
                onStartRecording = {},
                onStopRecording = {}
            )

            RecordButton(
                speechState = SpeechRecognitionResult.Listening,
                isGeneratingEmbedding = false,
                onStartRecording = {},
                onStopRecording = {}
            )

            RecordButton(
                speechState = SpeechRecognitionResult.Processing,
                isGeneratingEmbedding = false,
                onStartRecording = {},
                onStopRecording = {}
            )

            RecordButton(
                speechState = SpeechRecognitionResult.Idle,
                isGeneratingEmbedding = true,
                onStartRecording = {},
                onStopRecording = {}
            )
        }
    }
}