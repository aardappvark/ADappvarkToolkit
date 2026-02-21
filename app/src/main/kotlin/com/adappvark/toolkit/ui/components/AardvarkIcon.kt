package com.adappvark.toolkit.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.ui.theme.SolanaGreen
import com.adappvark.toolkit.ui.theme.SolanaPurple

/**
 * Custom Aardvark icon - stylized long nose silhouette
 * Represents the AardAppvark brand identity
 */
@Composable
fun AardvarkIcon(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    filled: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()

        // Scale factors
        val scaleX = width / 100f
        val scaleY = height / 100f

        // Create aardvark head/nose silhouette path
        val path = Path().apply {
            // Start at the back of the head
            moveTo(85f * scaleX, 35f * scaleY)

            // Top of head curve
            cubicTo(
                80f * scaleX, 20f * scaleY,
                60f * scaleX, 15f * scaleY,
                45f * scaleX, 20f * scaleY
            )

            // Long nose - top edge
            cubicTo(
                30f * scaleX, 22f * scaleY,
                15f * scaleX, 35f * scaleY,
                5f * scaleX, 45f * scaleY
            )

            // Nose tip
            cubicTo(
                2f * scaleX, 48f * scaleY,
                2f * scaleX, 52f * scaleY,
                5f * scaleX, 55f * scaleY
            )

            // Long nose - bottom edge
            cubicTo(
                15f * scaleX, 62f * scaleY,
                30f * scaleX, 58f * scaleY,
                45f * scaleX, 55f * scaleY
            )

            // Jaw/chin
            cubicTo(
                55f * scaleX, 58f * scaleY,
                65f * scaleX, 65f * scaleY,
                75f * scaleX, 60f * scaleY
            )

            // Back to start (neck area)
            cubicTo(
                82f * scaleX, 55f * scaleY,
                88f * scaleX, 45f * scaleY,
                85f * scaleX, 35f * scaleY
            )

            close()
        }

        // Draw ear
        val earPath = Path().apply {
            moveTo(70f * scaleX, 25f * scaleY)
            cubicTo(
                75f * scaleX, 10f * scaleY,
                85f * scaleX, 8f * scaleY,
                88f * scaleX, 20f * scaleY
            )
            cubicTo(
                90f * scaleX, 28f * scaleY,
                85f * scaleX, 32f * scaleY,
                78f * scaleX, 30f * scaleY
            )
            close()
        }

        // Draw eye
        val eyeCenter = Offset(62f * scaleX, 38f * scaleY)
        val eyeRadius = 4f * scaleX

        if (filled) {
            drawPath(path, color)
            drawPath(earPath, color)
            // Eye highlight (small circle)
            drawCircle(
                color = Color.White,
                radius = eyeRadius,
                center = eyeCenter
            )
            drawCircle(
                color = color.copy(alpha = 0.8f),
                radius = eyeRadius * 0.6f,
                center = eyeCenter
            )
        } else {
            drawPath(
                path,
                color,
                style = Stroke(
                    width = 3f * scaleX,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            drawPath(
                earPath,
                color,
                style = Stroke(
                    width = 3f * scaleX,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            drawCircle(
                color = color,
                radius = eyeRadius,
                center = eyeCenter,
                style = Stroke(width = 2f * scaleX)
            )
        }
    }
}

/**
 * Simplified aardvark nose icon - just the distinctive long snout
 * Good for smaller sizes like list items
 */
@Composable
fun AardvarkNoseIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()

        val scaleX = width / 100f
        val scaleY = height / 100f

        // Simple elongated nose shape
        val path = Path().apply {
            // Start at back
            moveTo(95f * scaleX, 40f * scaleY)

            // Top curve to nose tip
            cubicTo(
                70f * scaleX, 25f * scaleY,
                30f * scaleX, 30f * scaleY,
                5f * scaleX, 50f * scaleY
            )

            // Bottom curve back
            cubicTo(
                30f * scaleX, 70f * scaleY,
                70f * scaleX, 75f * scaleY,
                95f * scaleX, 60f * scaleY
            )

            close()
        }

        drawPath(path, color)
    }
}

/**
 * Animated Aardvark icon with breathing glow pulse and gradient fill.
 * Used on the entry screen and home screen hero sections.
 */
@Composable
fun AnimatedAardvarkIcon(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    glowColor: Color = SolanaPurple
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aardvark_glow")

    // Breathing pulse for glow radius
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Subtle scale breathing
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Gradient shift for the fill
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_shift"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size).scale(scale)) {
            val width = size.toPx()
            val height = size.toPx()
            val scaleX = width / 100f
            val scaleY = height / 100f

            // Draw glow circle behind
            drawCircle(
                color = glowColor.copy(alpha = glowAlpha),
                radius = width * 0.55f,
                center = Offset(width / 2f, height / 2f)
            )

            // Secondary glow ring
            drawCircle(
                color = SolanaGreen.copy(alpha = glowAlpha * 0.4f),
                radius = width * 0.45f,
                center = Offset(width / 2f, height / 2f)
            )

            // Aardvark head path (same as AardvarkIcon)
            val path = Path().apply {
                moveTo(85f * scaleX, 35f * scaleY)
                cubicTo(80f * scaleX, 20f * scaleY, 60f * scaleX, 15f * scaleY, 45f * scaleX, 20f * scaleY)
                cubicTo(30f * scaleX, 22f * scaleY, 15f * scaleX, 35f * scaleY, 5f * scaleX, 45f * scaleY)
                cubicTo(2f * scaleX, 48f * scaleY, 2f * scaleX, 52f * scaleY, 5f * scaleX, 55f * scaleY)
                cubicTo(15f * scaleX, 62f * scaleY, 30f * scaleX, 58f * scaleY, 45f * scaleX, 55f * scaleY)
                cubicTo(55f * scaleX, 58f * scaleY, 65f * scaleX, 65f * scaleY, 75f * scaleX, 60f * scaleY)
                cubicTo(82f * scaleX, 55f * scaleY, 88f * scaleX, 45f * scaleY, 85f * scaleX, 35f * scaleY)
                close()
            }

            // Ear path
            val earPath = Path().apply {
                moveTo(70f * scaleX, 25f * scaleY)
                cubicTo(75f * scaleX, 10f * scaleY, 85f * scaleX, 8f * scaleY, 88f * scaleX, 20f * scaleY)
                cubicTo(90f * scaleX, 28f * scaleY, 85f * scaleX, 32f * scaleY, 78f * scaleX, 30f * scaleY)
                close()
            }

            // Draw with gradient fill
            val gradientBrush = Brush.linearGradient(
                colors = listOf(SolanaPurple, SolanaGreen, SolanaPurple),
                start = Offset(gradientOffset * width, 0f),
                end = Offset(width * (1f - gradientOffset), height)
            )
            drawPath(path, gradientBrush)
            drawPath(earPath, gradientBrush)

            // Eye
            val eyeCenter = Offset(62f * scaleX, 38f * scaleY)
            val eyeRadius = 4f * scaleX
            drawCircle(color = Color.White, radius = eyeRadius, center = eyeCenter)
            drawCircle(color = Color(0xFF0A0A12), radius = eyeRadius * 0.55f, center = eyeCenter)
        }
    }
}
