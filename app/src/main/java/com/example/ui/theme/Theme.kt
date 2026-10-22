package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Unwraps wrapped contexts so the host Activity window can always be reached. */
private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val LightColorScheme = lightColorScheme(
    primary = ClaudeTerracotta,
    secondary = ClaudeSoftTomato,
    background = ClaudeAlabaster,
    surface = ClaudeWhite,
    onPrimary = Color.White,
    onSecondary = ClaudeCharcoal,
    onBackground = ClaudeCharcoal,
    onSurface = ClaudeCharcoal,
    outline = ClaudeSandBorder,
    surfaceVariant = ClaudeSelectedBg,
    onSurfaceVariant = ClaudeTerracotta
)

private val DarkColorScheme = darkColorScheme(
    primary = ClaudeTerracotta,
    secondary = ClaudeOrangeAccent,
    background = ClaudeOnyx,
    surface = ClaudeDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = ClaudeDarkText,
    onSurface = ClaudeDarkText,
    outline = ClaudeDarkBorder,
    surfaceVariant = ClaudeDarkSelectedBg,
    onSurfaceVariant = ClaudeOrangeAccent
)

@Composable
fun FilesClawTheme(
    themeSetting: String = "Light", // "Light", "Dark", "System"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Keep the system status/navigation bar icon contrast in sync with the IN-APP theme.
    // enableEdgeToEdge() derives the icon appearance from the SYSTEM dark-mode flag once at
    // Activity creation, so on a dark-mode phone running the app in Light theme the status
    // bar icons stayed white over the near-white Alabaster canvas and became invisible.
    // Re-asserting here (a) fixes the very first composition and (b) re-fires whenever the
    // user flips the in-app theme in Settings, keeping icon color and background aligned.
    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = context.findActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalGlass provides if (darkTheme) DarkGlass else LightGlass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
