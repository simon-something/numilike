package com.numilike.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightBackground,
    onBackground = LightInputText,
    onSurface = LightInputText,
    outline = LightDivider,
    error = LightError,
)

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkBackground,
    onBackground = DarkInputText,
    onSurface = DarkInputText,
    outline = DarkDivider,
    error = DarkError,
)

@Composable
fun NumiLikeTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NumiLikeTypography,
        content = content,
    )
}
