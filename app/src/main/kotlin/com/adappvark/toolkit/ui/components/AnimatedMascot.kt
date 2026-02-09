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
 * Shows Varky in different states using vector drawables
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
        val drawableId = when (state) {
            MascotState.IDLE, MascotState.THINKING -> R.drawable.varky_idle
            MascotState.WORKING -> R.drawable.varky_working
            MascotState.SUCCESS -> R.drawable.varky_success
            MascotState.ERROR -> R.drawable.varky_idle  // Use idle for error state too
        }

        Image(
            painter = painterResource(id = drawableId),
            contentDescription = when (state) {
                MascotState.IDLE, MascotState.THINKING -> "Varky idle"
                MascotState.WORKING -> "Varky working"
                MascotState.SUCCESS -> "Varky celebrating"
                MascotState.ERROR -> "Varky"
            },
            modifier = Modifier.size(size)
        )

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
