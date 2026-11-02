package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IncomeEmerald,
    onPrimary = Color.White,
    primaryContainer = IncomeEmeraldContainer,
    onPrimaryContainer = Color.White,
    secondary = AccentIndigo,
    onSecondary = Color.White,
    tertiary = AccentCyan,
    background = SlateDarkBackground,
    onBackground = TextPrimaryDark,
    surface = SlateCardBackground,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateCardBorder,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateCardBorder,
    error = ExpenseCrimson,
    onError = Color.White,
    errorContainer = ExpenseCrimsonContainer,
    onErrorContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IncomeEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = AccentIndigo,
    onSecondary = Color.White,
    tertiary = AccentCyan,
    background = SlateLightBackground,
    onBackground = TextPrimaryLight,
    surface = SlateLightCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = SlateLightBorder,
    onSurfaceVariant = TextSecondaryLight,
    outline = SlateLightBorder,
    error = ExpenseCrimson,
    onError = Color.White
)

@Composable
fun ZenithFinanceTheme(
    darkTheme: Boolean = true, // Default to dark theme for premium fintech aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

