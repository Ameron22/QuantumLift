package com.example.gymtracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define color schemes using colors from colors.xml
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE), // purple_500
    secondary = Color(0xFF03DAC5), // teal_200
    background = Color(0xFFFFFFFF), // white
    surface = Color(0xFFFFFFFF), // white
    onPrimary = Color(0xFFFFFFFF), // white
    onSecondary = Color(0xFF000000), // black
    onBackground = Color(0xFF000000), // black
    onSurface = Color(0xFF000000) // black
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC), // purple_200
    secondary = Color(0xFF03DAC5), // teal_200
    background = Color(0xFF000000), // black
    surface = Color(0xFF000000), // black
    onPrimary = Color(0xFF000000), // black
    onSecondary = Color(0xFFFFFFFF), // white
    onBackground = Color(0xFFFFFFFF), // white
    onSurface = Color(0xFFFFFFFF) // white
)

@Composable
fun QuantumLiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
           // window.statusBarColor = DarkerBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}