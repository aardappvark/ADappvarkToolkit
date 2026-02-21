package com.adappvark.toolkit.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==================== Glass Color Constants ====================

val GlassBorder = Color.White.copy(alpha = 0.12f)
val GlassBorderLight = Color.White.copy(alpha = 0.18f)
val GlassBackground = Color.White.copy(alpha = 0.06f)
val GlassBackgroundElevated = Color.White.copy(alpha = 0.10f)
val GlassBackgroundHighlight = Color.White.copy(alpha = 0.14f)
val SolanaPurpleGlow = SolanaPurple.copy(alpha = 0.3f)
val SolanaGreenGlow = SolanaGreen.copy(alpha = 0.25f)

// Deep space background colors for gradient
val DeepSpace1 = Color(0xFF0A0A12)
val DeepSpace2 = Color(0xFF0E0B1E)
val DeepSpace3 = Color(0xFF120C24)
val DeepSpacePurple = Color(0xFF1A0E30)

// ==================== Glass Card Composable ====================

/**
 * A glassmorphism-styled card with semi-transparent background,
 * gradient border, and optional glow effect.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevated: Boolean = false,
    glowColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val bgColor = if (elevated) GlassBackgroundElevated else GlassBackground
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.02f),
            Color.White.copy(alpha = 0.08f)
        )
    )

    val glowModifier = if (glowColor != null) {
        Modifier.drawBehind {
            drawCircle(
                color = glowColor.copy(alpha = 0.08f),
                radius = size.maxDimension * 0.6f,
                center = center
            )
        }
    } else Modifier

    Box(modifier = glowModifier.then(modifier)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    width = 1.dp,
                    brush = borderBrush,
                    shape = shape
                ),
            shape = shape,
            color = bgColor
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

// ==================== Animated Gradient Background ====================

/**
 * Full-screen animated gradient background with slowly shifting colors.
 * Creates a premium dark space atmosphere.
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_gradient")

    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            DeepSpace1,
            lerp(DeepSpace2, DeepSpacePurple, offset),
            lerp(DeepSpace3, DeepSpace1, offset),
            DeepSpace2
        ),
        start = Offset(0f, 0f),
        end = Offset(
            Float.POSITIVE_INFINITY * (0.5f + offset * 0.5f),
            Float.POSITIVE_INFINITY
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush),
        content = content
    )
}

/**
 * Simple color interpolation
 */
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}

// ==================== Glass Divider ====================

/**
 * A thin gradient-opacity divider with glass effect.
 */
@Composable
fun GlassDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.12f),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(gradientBrush)
    )
}

// ==================== Modifier Extensions ====================

/**
 * Adds a glowing border effect with Solana purple/green gradient.
 */
fun Modifier.glowBorder(
    shape: Shape = RoundedCornerShape(16.dp),
    glowAlpha: Float = 0.4f
): Modifier = this.border(
    width = 1.5.dp,
    brush = Brush.linearGradient(
        colors = listOf(
            SolanaPurple.copy(alpha = glowAlpha),
            SolanaGreen.copy(alpha = glowAlpha * 0.7f),
            SolanaPurple.copy(alpha = glowAlpha * 0.5f)
        )
    ),
    shape = shape
)

/**
 * Adds a subtle gradient border for glass elements.
 */
fun Modifier.gradientBorder(
    shape: Shape = RoundedCornerShape(16.dp),
    alpha: Float = 0.15f
): Modifier = this.border(
    width = 1.dp,
    brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = alpha),
            Color.White.copy(alpha = alpha * 0.3f),
            Color.White.copy(alpha = alpha * 0.1f),
            Color.White.copy(alpha = alpha * 0.5f)
        )
    ),
    shape = shape
)

/**
 * Solana gradient brush for text or icons.
 */
val SolanaGradientBrush = Brush.linearGradient(
    colors = listOf(SolanaPurple, SolanaGreen)
)

/**
 * Purple glow gradient brush.
 */
val PurpleGlowBrush = Brush.radialGradient(
    colors = listOf(
        SolanaPurple.copy(alpha = 0.15f),
        SolanaPurple.copy(alpha = 0.05f),
        Color.Transparent
    )
)

/**
 * Green glow gradient brush.
 */
val GreenGlowBrush = Brush.radialGradient(
    colors = listOf(
        SolanaGreen.copy(alpha = 0.12f),
        SolanaGreen.copy(alpha = 0.04f),
        Color.Transparent
    )
)
