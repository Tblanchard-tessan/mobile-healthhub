package com.example.smart_watch_hub.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin

/**
 * Animation utilities and constants for consistent animations throughout the app.
 */
object AnimationConstants {
    // Durations
    const val FAST = 200
    const val NORMAL = 300
    const val SLOW = 500
    const val VERY_SLOW = 800

    // Delays
    const val SHORT_DELAY = 50
    const val MEDIUM_DELAY = 100
    const val LONG_DELAY = 150

    // Easing
    val FastOutSlowInEasing: Easing = androidx.compose.animation.core.FastOutSlowInEasing
    val EaseInOutCubic: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
    val SpringDefault: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Fade in animation with optional delay.
 */
@Composable
fun FadeInAnimation(
    visible: Boolean,
    durationMillis: Int = AnimationConstants.NORMAL,
    delayMillis: Int = 0,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween<Float>(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = AnimationConstants.FastOutSlowInEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween<Float>(
                durationMillis = durationMillis,
                easing = AnimationConstants.FastOutSlowInEasing
            )
        ),
        content = content
    )
}

/**
 * Slide up and fade in animation for cards.
 */
@Composable
fun SlideUpFadeIn(
    visible: Boolean,
    durationMillis: Int = AnimationConstants.NORMAL,
    delayMillis: Int = 0,
    slideDistance: Int = 50,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = AnimationConstants.EaseInOutCubic
            ),
            initialOffsetY = { slideDistance }
        ) + fadeIn(
            animationSpec = tween<Float>(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = AnimationConstants.FastOutSlowInEasing
            )
        ),
        exit = slideOutVertically(
            animationSpec = tween(
                durationMillis = durationMillis / 2,
                easing = AnimationConstants.FastOutSlowInEasing
            ),
            targetOffsetY = { -slideDistance }
        ) + fadeOut(
            animationSpec = tween<Float>(
                durationMillis = durationMillis / 2,
                easing = AnimationConstants.FastOutSlowInEasing
            )
        ),
        content = content
    )
}

/**
 * Scale animation for buttons and interactive elements.
 */
@Composable
fun ScaleAnimation(
    targetScale: Float = 0.95f,
    durationMillis: Int = AnimationConstants.FAST,
    content: @Composable (Modifier) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    content(Modifier.scale(scale))
}

/**
 * Pulsing animation for loading indicators.
 */
@Composable
fun PulseAnimation(
    minScale: Float = 0.9f,
    maxScale: Float = 1.1f,
    durationMillis: Int = 1000,
    content: @Composable (Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    content(scale)
}

/**
 * Shimmer effect for loading states.
 */
@Composable
fun ShimmerEffect(
    durationMillis: Int = 1500,
    content: @Composable (Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    content(shimmer)
}

/**
 * Staggered list animation for cards.
 */
@Composable
fun StaggeredCardAnimation(
    index: Int,
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    SlideUpFadeIn(
        visible = visible,
        durationMillis = AnimationConstants.NORMAL,
        delayMillis = index * AnimationConstants.SHORT_DELAY,
        slideDistance = 30,
        content = content
    )
}

/**
 * Value change animation for numeric displays.
 */
@Composable
fun AnimatedNumber(
    targetValue: Int,
    durationMillis: Int = AnimationConstants.SLOW,
    content: @Composable (Int) -> Unit
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = AnimationConstants.EaseInOutCubic
        )
    )

    content(animatedValue)
}

/**
 * Value change animation for float displays.
 */
@Composable
fun AnimatedFloat(
    targetValue: Float,
    durationMillis: Int = AnimationConstants.SLOW,
    content: @Composable (Float) -> Unit
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = AnimationConstants.EaseInOutCubic
        )
    )

    content(animatedValue)
}
