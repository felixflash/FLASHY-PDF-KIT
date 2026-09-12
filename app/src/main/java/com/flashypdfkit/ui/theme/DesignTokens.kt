package com.flashypdfkit.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Option 2: Sunset Coral & Indigo Navy ("Vibrant Modernist") Design Tokens
 * Friendly, crisp, energetic. High-contrast legibility with warm vibrant personality.
 */
object ActivePalette {
    // Primary: Sunset Coral
    val Primary = Color(0xFFF26457)
    val PrimaryHover = Color(0xFFE25245)
    val PrimarySubtleLight = Color(0xFFFEEFEB)
    val PrimarySubtleDark = Color(0xFF381F1D)

    // Deep Accent: Indigo Navy
    val Navy = Color(0xFF2E3A59)
    val Secondary = Navy
    val NavySubtleLight = Color(0xFFEDEFF5)
    val NavySubtleDark = Color(0xFF1B2338)

    // Vibrant Accents
    val Honey = Color(0xFFF3A738)
    val HoneySubtleLight = Color(0xFFFDF6EC)
    val HoneySubtleDark = Color(0xFF382914)

    val Teal = Color(0xFF2EA59C)
    val TealSubtleLight = Color(0xFFEAF6F5)
    val TealSubtleDark = Color(0xFF142B28)

    val Indigo = Color(0xFF5D5FEF)
    val IndigoSubtleLight = Color(0xFFEEEDFD)
    val IndigoSubtleDark = Color(0xFF1D1F3D)

    val Danger = Color(0xFFE84545)
    val Success = Color(0xFF2DBB7B)

    // Light Theme Surfaces (Warm Porcelain & Crisp Alabaster)
    val LightCanvas = Color(0xFFF7F8FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceMuted = Color(0xFFF0F2F6)
    val LightBorder = Color(0xFFE4E7EE)
    val LightTextPrimary = Color(0xFF172033)
    val LightTextMuted = Color(0xFF5B6A87)
    val LightTextTertiary = Color(0xFF8F9BB3)

    // Dark Theme Surfaces (Deep Ink Midnight & Indigo Slate)
    val DarkCanvas = Color(0xFF0F131D)
    val DarkSurface = Color(0xFF171E2D)
    val DarkSurfaceMuted = Color(0xFF212A3E)
    val DarkBorder = Color(0xFF2C374F)
    val DarkTextPrimary = Color(0xFFF5F8FC)
    val DarkTextMuted = Color(0xFF98A6C0)
    val DarkTextTertiary = Color(0xFF6B7A99)

    // Gradients
    val PrimaryGradient = Brush.linearGradient(
        listOf(Color(0xFFF26457), Color(0xFFF87A6F))
    )
    val SunsetGradient = Brush.linearGradient(
        listOf(Color(0xFFF26457), Color(0xFFF3A738))
    )
    val NavyGradient = Brush.linearGradient(
        listOf(Color(0xFF2E3A59), Color(0xFF1C253B))
    )
    val TealGradient = Brush.linearGradient(
        listOf(Color(0xFF2EA59C), Color(0xFF208A82))
    )
}

object AppSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 44.dp
}

object AppRadius {
    val sm: Dp = 8.dp
    val md: Dp = 14.dp
    val lg: Dp = 20.dp
    val xl: Dp = 28.dp
    val full: Dp = 999.dp
}

/**
 * Tactile touch press micro-interaction:
 * Provides physical spring feedback when pressing buttons or cards.
 */
fun Modifier.tactilePress(
    enabled: Boolean = true,
    targetScale: Float = 0.965f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "tactileScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}
