package com.example.smart_watch_hub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.smart_watch_hub.ui.theme.HealthBlue80
import com.example.smart_watch_hub.ui.theme.HealthBlue30
import com.example.smart_watch_hub.ui.theme.HealthBlue90
import com.example.smart_watch_hub.ui.theme.HealthBlue40
import com.example.smart_watch_hub.ui.theme.FitnessGreen80
import com.example.smart_watch_hub.ui.theme.FitnessGreen30
import com.example.smart_watch_hub.ui.theme.FitnessGreen90
import com.example.smart_watch_hub.ui.theme.FitnessGreen40
import com.example.smart_watch_hub.ui.theme.TechPurple80
import com.example.smart_watch_hub.ui.theme.TechPurple30
import com.example.smart_watch_hub.ui.theme.TechPurple90
import com.example.smart_watch_hub.ui.theme.TechPurple40
import com.example.smart_watch_hub.ui.theme.ErrorRed80
import com.example.smart_watch_hub.ui.theme.ErrorRed30
import com.example.smart_watch_hub.ui.theme.ErrorRed90
import com.example.smart_watch_hub.ui.theme.ErrorRed40
import com.example.smart_watch_hub.ui.theme.SurfaceDark
import com.example.smart_watch_hub.ui.theme.SurfaceVariantDark
import com.example.smart_watch_hub.ui.theme.OutlineDark
import com.example.smart_watch_hub.ui.theme.SurfaceLight
import com.example.smart_watch_hub.ui.theme.SurfaceVariantLight
import com.example.smart_watch_hub.ui.theme.OutlineLight
import com.example.smart_watch_hub.ui.theme.Shapes
import com.example.smart_watch_hub.ui.theme.Typography as ThemeTypography

private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = HealthBlue80,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = HealthBlue30,
    onPrimaryContainer = HealthBlue90,

    // Secondary colors
    secondary = FitnessGreen80,
    onSecondary = Color(0xFF1C1C1C),
    secondaryContainer = FitnessGreen30,
    onSecondaryContainer = FitnessGreen90,

    // Tertiary colors
    tertiary = TechPurple80,
    onTertiary = Color(0xFF3E0052),
    tertiaryContainer = TechPurple30,
    onTertiaryContainer = TechPurple90,

    // Error colors
    error = ErrorRed80,
    onError = Color(0xFF690005),
    errorContainer = ErrorRed30,
    onErrorContainer = ErrorRed90,

    // Background & Surface
    background = SurfaceDark,
    onBackground = Color(0xFFE6E1E5),
    surface = SurfaceDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCAC4D0),

    // Outline
    outline = OutlineDark,
    outlineVariant = Color(0xFF49454F),

    // Other
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = HealthBlue40,
    surfaceTint = HealthBlue80
)

private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = HealthBlue40,
    onPrimary = Color.White,
    primaryContainer = HealthBlue90,
    onPrimaryContainer = HealthBlue30,

    // Secondary colors
    secondary = FitnessGreen40,
    onSecondary = Color(0xFF1C1C1C),
    secondaryContainer = FitnessGreen90,
    onSecondaryContainer = FitnessGreen30,

    // Tertiary colors
    tertiary = TechPurple40,
    onTertiary = Color.White,
    tertiaryContainer = TechPurple90,
    onTertiaryContainer = TechPurple30,

    // Error colors
    error = ErrorRed40,
    onError = Color.White,
    errorContainer = ErrorRed90,
    onErrorContainer = ErrorRed30,

    // Background & Surface
    background = SurfaceLight,
    onBackground = Color(0xFF1C1B1F),
    surface = SurfaceLight,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF49454F),

    // Outline
    outline = OutlineLight,
    outlineVariant = Color(0xFFCAC4D0),

    // Other
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = HealthBlue80,
    surfaceTint = HealthBlue40
)

@Composable
fun Smart_Watch_HUBTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,  // Disabled by default for consistent health theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.setStatusBarColor(colorScheme.primary.toArgb())
            WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ThemeTypography,
        shapes = Shapes,
        content = content
    )
}