package com.example.transit_app.ui.theme

import android.app.Activity
import android.os.Build
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

// Eye-Friendly Light Theme Colors
val SoftOffWhite = Color(0xFFF8F9FA)
val WarmCream = Color(0xFFFDFCF8)
val CharcoalText = Color(0xFF1E293B)
val SlateSecondaryText = Color(0xFF475569)
val MutedDeepBlue = Color(0xFF335C85)
val SurfaceContainerHighLight = Color(0xFFF1F5F9)

// High-Contrast Dark Theme Colors (for reference)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val LightText = Color(0xFFE2E8F0)
val NeonCyan = Color(0xFF38BDF8)

private val LowGlareLightColorScheme = lightColorScheme(
    primary = MutedDeepBlue,
    onPrimary = Color.White,
    background = SoftOffWhite,
    onBackground = CharcoalText,
    surface = WarmCream,
    onSurface = CharcoalText,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = SlateSecondaryText,
    surfaceContainerLow = SoftOffWhite,
    surfaceContainerHigh = SurfaceContainerHighLight
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    background = DarkBackground,
    onBackground = LightText,
    surface = DarkSurface,
    onSurface = LightText,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun TransitAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to enforce our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> HighContrastDarkColorScheme
        else -> LowGlareLightColorScheme
    }

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
        typography = Typography, // Ensure you have your Typography defined
        content = content
    )
}