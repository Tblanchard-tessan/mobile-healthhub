package com.example.smart_watch_hub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smart_watch_hub.ui.theme.Spacing
import com.example.smart_watch_hub.ui.animations.PulseAnimation

/**
 * Full-screen loading indicator with optional message.
 *
 * Usage:
 * LoadingIndicator(message = "Loading health data...")
 */
@Composable
fun LoadingIndicator(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.apply { androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp)) }
        )
    }
}

/**
 * Compact loading indicator for inline use.
 *
 * Usage:
 * Row {
 *     CompactLoadingIndicator()
 *     Text("Processing...")
 * }
 */
@Composable
fun CompactLoadingIndicator(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 16.dp
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        strokeWidth = 1.dp,
        color = MaterialTheme.colorScheme.primary
    )
}
