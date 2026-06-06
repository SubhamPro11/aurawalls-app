package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuraColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonViolet,
    tertiary = AccentPink,
    background = AuraBackground,
    surface = AuraSurface,
    surfaceVariant = AuraSurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = AuraBorder
)

@Composable
fun MyApplicationTheme(
    // Force dark theme as AuraWalls is exclusively a deep cinematic dark UI
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraColorScheme,
        typography = Typography,
        content = content
    )
}
