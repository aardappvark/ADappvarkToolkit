package com.adappvark.toolkit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Solana Brand Colors
val SolanaPurple = Color(0xFF9945FF)
val SolanaGreen = Color(0xFF14F195)
val AardvarkBrown = Color(0xFF8B6F47)
val AardvarkTan = Color(0xFFD4A574)

private val LightColorScheme = lightColorScheme(
    primary = SolanaPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    
    secondary = SolanaGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F9E6),
    onSecondaryContainer = Color(0xFF002114),
    
    tertiary = AardvarkBrown,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5E7D3),
    onTertiaryContainer = Color(0xFF2C1F0A),
    
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    
    secondary = SolanaGreen,
    onSecondary = Color(0xFF003825),
    secondaryContainer = Color(0xFF005236),
    onSecondaryContainer = Color(0xFFD0F9E6),
    
    tertiary = AardvarkTan,
    onTertiary = Color(0xFF3F2E13),
    tertiaryContainer = Color(0xFF57432A),
    onTertiaryContainer = Color(0xFFF5E7D3),
    
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

@Composable
fun ADappvarkToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
