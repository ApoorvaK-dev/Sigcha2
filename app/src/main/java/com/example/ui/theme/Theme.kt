package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MinimalPrimaryDark,
    secondary = MinimalSecondaryDark,
    background = MinimalBgDark,
    surface = MinimalBgDark,
    onPrimary = MinimalBgDark,
    onSecondary = MinimalTextPrimaryDark,
    onBackground = MinimalTextPrimaryDark,
    onSurface = MinimalTextPrimaryDark,
    surfaceVariant = MinimalSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = MinimalPrimary,
    secondary = MinimalSecondary,
    tertiary = MinimalTertiary,
    background = MinimalBg,
    surface = MinimalSurface,
    onPrimary = java.lang.Long.decode("0xFFFFFFFF").let { androidx.compose.ui.graphics.Color(it) },
    onSecondary = MinimalOnPrimaryContainer,
    onBackground = MinimalTextPrimary,
    onSurface = MinimalTextPrimary,
    surfaceVariant = MinimalSurfaceVariant,
    outlineVariant = MinimalOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
