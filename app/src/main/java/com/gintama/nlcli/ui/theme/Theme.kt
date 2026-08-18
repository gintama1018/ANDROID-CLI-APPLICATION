package com.gintama.nlcli.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TerminalGreen,
    onPrimary = TerminalBackground,
    primaryContainer = TerminalSurfaceVariant,
    onPrimaryContainer = TerminalGreen,
    secondary = TerminalCyan,
    onSecondary = TerminalBackground,
    secondaryContainer = TerminalSurfaceVariant,
    onSecondaryContainer = TerminalCyan,
    tertiary = TerminalAmber,
    background = TerminalBackground,
    onBackground = TextBright,
    surface = TerminalSurface,
    onSurface = TextBright,
    surfaceVariant = TerminalSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = TerminalBorder,
    error = TerminalRed,
    onError = TextBright
)

@Composable
fun NLCLITheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TerminalBackground.toArgb()
            window.navigationBarColor = TerminalBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
