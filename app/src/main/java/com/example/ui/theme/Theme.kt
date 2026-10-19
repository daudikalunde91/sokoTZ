package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val SokoDarkColorScheme = darkColorScheme(
    primary = SokoNeonBlue,
    onPrimary = SokoBlueBlack,
    primaryContainer = SokoMidnightBlue,
    onPrimaryContainer = SokoLightText,
    secondary = SokoNeonViolet,
    onSecondary = SokoBlueBlack,
    secondaryContainer = SokoDeepPurple,
    onSecondaryContainer = SokoLightText,
    tertiary = SokoElectricBlue,
    onTertiary = SokoBlueBlack,
    background = SokoBlueBlack,
    onBackground = SokoLightText,
    surface = SokoDeepNavy,
    onSurface = SokoLightText,
    surfaceVariant = SokoMidnightBlue,
    onSurfaceVariant = SokoMutedText,
    outline = SokoCardBorder,
    outlineVariant = SokoMidnightBlue,
    error = SokoError,
    onError = Color.White
)

object SokoGradients {
    val HeroGradient = Brush.verticalGradient(
        colors = listOf(
            SokoMidnightBlue,
            SokoDeepNavy,
            SokoBlueBlack
        )
    )

    val AccentGradient = Brush.horizontalGradient(
        colors = listOf(
            SokoElectricBlue,
            SokoNeonBlue,
            SokoNeonViolet
        )
    )

    val PurpleNeonGradient = Brush.linearGradient(
        colors = listOf(
            SokoDeepPurple,
            SokoNeonViolet
        )
    )

    val CardHeaderGradient = Brush.horizontalGradient(
        colors = listOf(
            SokoMidnightBlue.copy(alpha = 0.8f),
            SokoDeepNavy.copy(alpha = 0.9f)
        )
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // SokoTZ signature Dark Neon aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SokoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
