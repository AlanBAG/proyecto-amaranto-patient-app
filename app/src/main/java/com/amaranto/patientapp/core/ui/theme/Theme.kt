package com.amaranto.patientapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AmarantoColorScheme = lightColorScheme(
    primary = AmarantoMagenta,
    onPrimary = CreamSurface,
    secondary = BrandGreen,
    onSecondary = CreamSurface,
    secondaryContainer = BrandGreenLight,
    onSecondaryContainer = CreamSurface,
    tertiary = BrandHeart,
    onTertiary = CreamSurface,
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = CreamSurface,
    onSurface = TextPrimary,
    surfaceVariant = CreamSurface,
    onSurfaceVariant = TextSecondary
)

@Composable
fun AmarantoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AmarantoColorScheme,
        content = content
    )
}