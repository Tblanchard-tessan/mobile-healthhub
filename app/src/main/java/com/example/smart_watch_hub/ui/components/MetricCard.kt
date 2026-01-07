package com.example.smart_watch_hub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smart_watch_hub.ui.theme.Spacing
import com.example.smart_watch_hub.ui.theme.CustomShapes

/**
 * Reusable metric card component for displaying health metrics.
 *
 * Usage:
 * MetricCard(
 *     label = "Heart Rate",
 *     value = "72",
 *     unit = "bpm",
 *     status = MetricStatus.NORMAL
 * )
 */
@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String = "",
    status: MetricStatus = MetricStatus.NORMAL,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        MetricStatus.NORMAL -> MaterialTheme.colorScheme.primary
        MetricStatus.WARNING -> Color(0xFFFF9800) // Orange
        MetricStatus.CRITICAL -> MaterialTheme.colorScheme.error
        MetricStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = statusColor
                )
            }
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Status indicator for metric values.
 */
enum class MetricStatus {
    NORMAL,
    WARNING,
    CRITICAL,
    UNKNOWN
}

/**
 * Helper function to determine metric status based on value.
 */
fun getHeartRateStatus(bpm: Int): MetricStatus {
    return when {
        bpm < 40 || bpm > 100 -> MetricStatus.WARNING
        bpm < 30 || bpm > 120 -> MetricStatus.CRITICAL
        bpm == 0 -> MetricStatus.UNKNOWN
        else -> MetricStatus.NORMAL
    }
}

fun getBloodOxygenStatus(spO2: Int): MetricStatus {
    return when {
        spO2 < 95 -> MetricStatus.WARNING
        spO2 < 90 -> MetricStatus.CRITICAL
        spO2 == 0 -> MetricStatus.UNKNOWN
        else -> MetricStatus.NORMAL
    }
}

fun getBloodPressureStatus(systolic: Int, diastolic: Int): MetricStatus {
    return when {
        systolic >= 180 || diastolic >= 120 -> MetricStatus.CRITICAL
        systolic >= 140 || diastolic >= 90 -> MetricStatus.WARNING
        systolic == 0 && diastolic == 0 -> MetricStatus.UNKNOWN
        else -> MetricStatus.NORMAL
    }
}

fun getBodyTemperatureStatus(tempC: Float): MetricStatus {
    return when {
        tempC > 38.5f -> MetricStatus.CRITICAL
        tempC > 37.5f || tempC < 36f -> MetricStatus.WARNING
        tempC <= 0f -> MetricStatus.UNKNOWN
        else -> MetricStatus.NORMAL
    }
}
