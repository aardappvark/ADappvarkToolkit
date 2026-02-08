package com.adappvark.toolkit.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adappvark.toolkit.R
import kotlinx.coroutines.delay

/**
 * Mascot states for Varky the ADappvark
 */
enum class MascotState {
    IDLE,
    WORKING,
    SUCCESS,
    ERROR,
    THINKING
}

/**
 * Animated mascot component
 * Shows Varky in different states with animation
 */
@Composable
fun AnimatedMascot(
    state: MascotState,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    message: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            MascotState.IDLE, MascotState.THINKING -> {
                Image(
                    painter = painterResource(id = R.drawable.mascot_idle),
                    contentDescription = "Varky idle",
                    modifier = Modifier.size(size)
                )
            }

            MascotState.WORKING -> {
                WorkingAnimation(modifier = Modifier, size = size)
            }

            MascotState.SUCCESS -> {
                Image(
                    painter = painterResource(id = R.drawable.mascot_success),
                    contentDescription = "Varky celebrating",
                    modifier = Modifier.size(size)
                )
            }

            MascotState.ERROR -> {
                Image(
                    painter = painterResource(id = R.drawable.mascot_error),
                    contentDescription = "Varky confused",
                    modifier = Modifier.size(size)
                )
            }
        }

        // Optional message
        if (message != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Working animation cycles through 3 frames
 */
@Composable
private fun WorkingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    var frame by remember { mutableIntStateOf(0) }

    // Cycle through frames
    LaunchedEffect(Unit) {
        while (true) {
            delay(300) // 300ms per frame = ~3 fps (retro feel)
            frame = (frame + 1) % 3
        }
    }

    val drawableId = when (frame) {
        0 -> R.drawable.mascot_working_1
        1 -> R.drawable.mascot_working_2
        else -> R.drawable.mascot_working_3
    }

    Image(
        painter = painterResource(id = drawableId),
        contentDescription = "Varky working",
        modifier = modifier.size(size)
    )
}
