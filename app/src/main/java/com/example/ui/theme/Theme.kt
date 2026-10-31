package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Here we connect our custom colors from Color.kt to Android's Material 3 theme slots.
// For example, "primary" is the main color used by buttons and the top bar,
// and "error" is the red color used when a batter gets out.
private val AppColorScheme = lightColorScheme(
    primary = CricketGreen,
    onPrimary = Color.White,
    primaryContainer = CricketGreenLight,
    onPrimaryContainer = CricketGreen,

    secondary = BoundaryOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = BoundaryOrange,

    error = WicketRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = WicketRed,

    background = ScreenBackground,
    onBackground = TextDark,
    surface = CardWhite,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = TextGray
)

// Wrapping our app inside MyApplicationTheme gives every screen access to these colors and fonts.
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
