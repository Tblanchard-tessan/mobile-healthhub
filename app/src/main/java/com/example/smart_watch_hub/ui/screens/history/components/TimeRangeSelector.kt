package com.example.smart_watch_hub.ui.screens.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smart_watch_hub.ui.screens.history.HistoryViewModel
import com.example.smart_watch_hub.ui.theme.Spacing
import com.example.smart_watch_hub.ui.theme.CustomShapes
import com.example.smart_watch_hub.ui.animations.SlideUpFadeIn

/**
 * Time range selector component for historical data.
 *
 * Allows switching between:
 * - 1 day (statistics only, no charts)
 * - 1 week (7-day data with charts)
 *
 * Usage:
 * TimeRangeSelector(
 *     selectedRange = HistoryViewModel.TimeRange.ONE_DAY,
 *     onRangeSelected = { viewModel.setTimeRange(it) }
 * )
 */
@Composable
fun TimeRangeSelector(
    selectedRange: HistoryViewModel.TimeRange,
    onRangeSelected: (HistoryViewModel.TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRangeButton(
                label = "1 day",
                selected = selectedRange == HistoryViewModel.TimeRange.ONE_DAY,
                onClick = { onRangeSelected(HistoryViewModel.TimeRange.ONE_DAY) },
                modifier = Modifier.weight(1f)
            )
            TimeRangeButton(
                label = "1 week",
                selected = selectedRange == HistoryViewModel.TimeRange.ONE_WEEK,
                onClick = { onRangeSelected(HistoryViewModel.TimeRange.ONE_WEEK) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual time range button.
 */
@Composable
private fun TimeRangeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = Color.White
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}
