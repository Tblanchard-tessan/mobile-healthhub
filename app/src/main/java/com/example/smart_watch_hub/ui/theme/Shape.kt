package com.example.smart_watch_hub.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Small components (buttons, chips)
    small = RoundedCornerShape(8.dp),

    // Medium components (cards, dialogs)
    medium = RoundedCornerShape(12.dp),

    // Large components (bottom sheets, large cards)
    large = RoundedCornerShape(16.dp),

    // Extra large (full-screen dialogs)
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Custom shapes for specific use cases throughout the app
 */
object CustomShapes {
    val MetricCard = RoundedCornerShape(12.dp)
    val ChartCard = RoundedCornerShape(16.dp)
    val StatusCard = RoundedCornerShape(12.dp)
    val DeviceCard = RoundedCornerShape(12.dp)
    val SleepCard = RoundedCornerShape(16.dp)
}
