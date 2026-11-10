package com.amaranto.patientapp.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmarantoColorScheme = lightColorScheme(
    primary = AmarantoPink,
    onPrimary = TextPrimary,
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

/** Color de acento legible para texto sobre fondos claros (el primario rosa no alcanza contraste suficiente). */
val ColorScheme.accentText: Color
    get() = AmarantoPinkText

@Composable
fun AmarantoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AmarantoColorScheme,
        content = content
    )
}