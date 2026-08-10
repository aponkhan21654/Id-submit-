package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberBluePrimary,
    onPrimary = Color.White,
    primaryContainer = CyberBlueContainer,
    onPrimaryContainer = CyberOnPrimaryContainer,
    secondary = CyberCyanSecondary,
    onSecondary = Color.White,
    secondaryContainer = CyberCyanContainer,
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = CyberMintTertiary,
    onTertiary = Color.White,
    tertiaryContainer = CyberMintContainer,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceHigh,
    outline = CyberOutline,
    outlineVariant = Color(0xFF1E293B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = CyberDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

