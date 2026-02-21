package com.adappvark.toolkit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Solana Brand Colors
val SolanaPurple = Color(0xFF9945FF)
val SolanaGreen = Color(0xFF14F195)
val AardvarkBrown = Color(0xFF8B6F47)
val AardvarkTan = Color(0xFFD4A574)

// Liquid Glass Dark Color Scheme — AMOLED-optimized deep space
private val GlassDarkColorScheme = darkColorScheme(
    primary = SolanaPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E1040),
    onPrimaryContainer = Color(0xFFEADDFF),

    secondary = SolanaGreen,
    onSecondary = Color(0xFF003825),
    secondaryContainer = Color(0xFF0A2A1C),
    onSecondaryContainer = Color(0xFFD0F9E6),

    tertiary = AardvarkTan,
    onTertiary = Color(0xFF3F2E13),
    tertiaryContainer = Color(0xFF2A1F10),
    onTertiaryContainer = Color(0xFFF5E7D3),

    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF0A0A12),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF12121C),
    onSurface = Color(0xFFE6E1E5),

    surfaceVariant = Color(0xFF1A1A2E),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

@Composable
fun ADappvarkToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force dark theme always — liquid glass only works on dark backgrounds
    val colorScheme = GlassDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
