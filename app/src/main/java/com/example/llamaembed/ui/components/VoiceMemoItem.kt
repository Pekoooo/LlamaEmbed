package com.example.llamaembed.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.llamaembed.data.local.VoiceMemoEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Voice memo item component with visual feedback
 *
 * Features:
 * - Material 3 design with proper elevation and colors
 * - Similarity score display for search results
 * - Long press selection
 * - Smooth animations and state transitions
 * - Accessibility support
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceMemoItem(
    memo: VoiceMemoEntity,
    onItemClick: (VoiceMemoEntity) -> Unit,
    onDeleteClick: (VoiceMemoEntity) -> Unit,
    onLongClick: (VoiceMemoEntity) -> Unit = {},
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }

    VoiceMemoContent(
        memo = memo,
        onItemClick = onItemClick,
        onDeleteClick = onDeleteClick,
        onLongClick = onLongClick,
        modifier = modifier,
        isSelected = isSelected,
        isExpanded = isExpanded,
        onExpandChange = { isExpanded = it }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VoiceMemoContent(
    memo: VoiceMemoEntity,
    onItemClick: (VoiceMemoEntity) -> Unit,
    onDeleteClick: (VoiceMemoEntity) -> Unit,
    onLongClick: (VoiceMemoEntity) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isExpanded: Boolean = false,
    onExpandChange: (Boolean) -> Unit = {}
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val maxLines = if (isExpanded) Int.MAX_VALUE else 3

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onItemClick(memo) },
                onLongClick = { onLongClick(memo) }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with timestamp and similarity score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatter.format(memo.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Duration indicator
                    if (memo.duration > 0) {
                        Text(
                            text = formatDuration(memo.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = { onDeleteClick(memo) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete memo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Memo text content
            Text(
                text = memo.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Expand/collapse button for long text
            if (memo.text.length > 150) {
                TextButton(
                    onClick = { onExpandChange(!isExpanded) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = if (isExpanded) "Show less" else "Show more",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Embedding status indicator
            if (memo.embedding != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ Processed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/**
 * Format duration from milliseconds to readable string
 */
private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}:${String.format("%02d", seconds % 60)}"
        else -> "${seconds / 3600}:${String.format("%02d", (seconds % 3600) / 60)}:${String.format("%02d", seconds % 60)}"
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceMemoItemPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Regular memo
            VoiceMemoItem(
                memo = VoiceMemoEntity(
                    id = 1,
                    text = "This is a sample voice memo with some content that demonstrates how the item looks in the list.",
                    timestamp = Date(),
                    embedding = null,
                    duration = 45000L
                ),
                onItemClick = {},
                onDeleteClick = {}
            )

            // Selected memo
            VoiceMemoItem(
                memo = VoiceMemoEntity(
                    id = 2,
                    text = "This is a longer voice memo that demonstrates the expandable functionality. It contains more text to show how the item handles longer content and provides a show more/less button for better user experience.",
                    timestamp = Date(System.currentTimeMillis() - 86400000), // Yesterday
                    embedding = null,
                    duration = 120000L
                ),
                onItemClick = {},
                onDeleteClick = {},
                isSelected = true
            )

            // Short memo without duration
            VoiceMemoItem(
                memo = VoiceMemoEntity(
                    id = 3,
                    text = "Short memo",
                    timestamp = Date(System.currentTimeMillis() - 3600000), // 1 hour ago
                    embedding = null,
                    duration = 0L
                ),
                onItemClick = {},
                onDeleteClick = {}
            )
        }
    }
}