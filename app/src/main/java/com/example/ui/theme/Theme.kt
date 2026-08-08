package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = Indigo100,
    secondary = ActiveCyan,
    onSecondary = Slate950,
    secondaryContainer = Cyan600,
    onSecondaryContainer = ActiveCyanContainer,
    tertiary = CoachAmber,
    onTertiary = Slate950,
    tertiaryContainer = Color(0xFF3A2600),
    onTertiaryContainer = Color(0xFFFFDEAC),
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate100,
    outline = Slate600,
    outlineVariant = Color(0xFF38485A),
    error = ErrorSoftRed,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFEAF1FF),
    inverseOnSurface = Color(0xFF213145),
    inversePrimary = Color(0xFFB7C8DE)
)

private val LightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = Color.White,
    primaryContainer = DeepNavyContainer,
    onPrimaryContainer = Color(0xFF8192A7),
    secondary = Color(0xFF006875),
    onSecondary = Color.White,
    secondaryContainer = ActiveCyan,
    onSecondaryContainer = Color(0xFF00616D),
    tertiary = Color(0xFF201300),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3A2600),
    onTertiaryContainer = Color(0xFFC08600),
    background = SoftBackground,
    onBackground = OnSurfacePrimary,
    surface = SurfaceLowest,
    onSurface = OnSurfacePrimary,
    surfaceVariant = SurfaceHighest,
    onSurfaceVariant = OnSurfaceVariant,
    outline = OutlineColor,
    outlineVariant = OutlineVariant,
    error = ErrorSoftRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    inverseSurface = Color(0xFF213145),
    inverseOnSurface = Color(0xFFEAF1FF),
    inversePrimary = Color(0xFFB7C8DE)
)

@Composable
fun SpeakCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
