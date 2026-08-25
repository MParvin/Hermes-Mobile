package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = HermesAmber,
    onPrimary = Slate950,
    primaryContainer = HermesAmberContainer,
    onPrimaryContainer = HermesAmberLight,
    secondary = HermesCyan,
    onSecondary = Slate950,
    secondaryContainer = HermesCyanContainer,
    onSecondaryContainer = HermesCyanLight,
    tertiary = HermesPurple,
    onTertiary = Slate950,
    tertiaryContainer = HermesPurpleContainer,
    onTertiaryContainer = Slate200,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    outline = Slate600,
    error = HermesDanger,
    onError = Slate50
)

private val LightColorScheme = darkColorScheme(
    // Hermes uses a polished dark cyber aesthetic by default for terminal & autonomous feel
    primary = HermesAmber,
    onPrimary = Slate950,
    primaryContainer = HermesAmberContainer,
    onPrimaryContainer = HermesAmberLight,
    secondary = HermesCyan,
    onSecondary = Slate950,
    secondaryContainer = HermesCyanContainer,
    onSecondaryContainer = HermesCyanLight,
    tertiary = HermesPurple,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    outline = Slate600,
    error = HermesDanger,
    onError = Slate50
)

@Composable
fun HermesMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep Hermes signature cyber palette
    content: @Composable () -> Unit,
) {
    HermesMobileTheme(darkTheme, dynamicColor, content)
}

