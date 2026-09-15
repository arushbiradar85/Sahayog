package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CoopPrimaryDark,
    onPrimary = CoopOnPrimaryDark,
    primaryContainer = CoopPrimaryContainerDark,
    onPrimaryContainer = CoopOnPrimaryContainerDark,
    background = CoopBackgroundDark,
    surface = CoopSurfaceDark,
    surfaceVariant = CoopSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = CoopPrimary,
    onPrimary = CoopOnPrimary,
    primaryContainer = CoopPrimaryContainer,
    onPrimaryContainer = CoopOnPrimaryContainer,
    secondary = CoopSecondary,
    onSecondary = CoopOnSecondary,
    secondaryContainer = CoopSecondaryContainer,
    onSecondaryContainer = CoopOnSecondaryContainer,
    tertiary = CoopTertiary,
    onTertiary = CoopOnTertiary,
    tertiaryContainer = CoopTertiaryContainer,
    onTertiaryContainer = CoopOnTertiaryContainer,
    background = CoopBackground,
    onBackground = CoopOnBackground,
    surface = CoopSurface,
    onSurface = CoopOnSurface,
    surfaceVariant = CoopSurfaceVariant,
    onSurfaceVariant = CoopOnSurfaceVariant,
    outline = CoopOutline
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

