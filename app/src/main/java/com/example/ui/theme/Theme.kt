package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.sokotz.data.model.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("sokotz_settings", Context.MODE_PRIVATE)
        val saved = prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        _themeMode.value = try {
            AppThemeMode.valueOf(saved)
        } catch (e: Exception) {
            AppThemeMode.DARK
        }
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        _themeMode.value = mode
        val prefs = context.getSharedPreferences("sokotz_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("theme_mode", mode.name).apply()
    }
}

val LocalIsDarkTheme = compositionLocalOf { true }

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

private val SokoLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = Color(0xFF2563EB),
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = SokoError,
    onError = Color.White
)

object SokoColors {
    val isDark: Boolean
        @Composable get() = LocalIsDarkTheme.current

    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background

    val surface: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val cardSurface: Color
        @Composable get() = if (isDark) SokoCardSurface else Color.White

    val cardBorder: Color
        @Composable get() = if (isDark) SokoCardBorder else Color(0xFFE2E8F0)

    val headerSurface: Color
        @Composable get() = if (isDark) SokoMidnightBlue else Color.White

    val navBarSurface: Color
        @Composable get() = if (isDark) SokoMidnightBlue else Color.White

    val textPrimary: Color
        @Composable get() = if (isDark) SokoLightText else Color(0xFF0F172A)

    val textMuted: Color
        @Composable get() = if (isDark) SokoMutedText else Color(0xFF475569)

    val inputBackground: Color
        @Composable get() = if (isDark) SokoDeepNavy else Color(0xFFF1F5F9)

    val accentPrimary: Color
        @Composable get() = if (isDark) SokoNeonBlue else Color(0xFF0284C7)

    val accentSecondary: Color
        @Composable get() = if (isDark) SokoNeonViolet else Color(0xFF7C3AED)

    val tagPillBackground: Color
        @Composable get() = if (isDark) SokoMidnightBlue else Color(0xFFE0F2FE)

    val heroGradient: Brush
        @Composable get() = if (isDark) {
            Brush.verticalGradient(listOf(SokoMidnightBlue, SokoDeepNavy, SokoBlueBlack))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFF1F5F9), Color(0xFFF8FAFC)))
        }
}

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
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) SokoDarkColorScheme else SokoLightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
