package com.example.smart_watch_hub.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 spacing system based on 4dp grid.
 * Use these values for consistent spacing throughout the app.
 */
object Spacing {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val mediumLarge: Dp = 16.dp
    val large: Dp = 20.dp
    val extraLarge: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
    val huge: Dp = 64.dp
}

/**
 * Screen-specific padding values
 */
object ScreenPadding {
    val horizontal: Dp = 16.dp
    val vertical: Dp = 16.dp
    val top: Dp = 8.dp
    val bottom: Dp = 16.dp
}

/**
 * Card-specific spacing
 */
object CardSpacing {
    val contentPadding: Dp = 16.dp
    val verticalSpacing: Dp = 12.dp
    val horizontalSpacing: Dp = 16.dp
}
