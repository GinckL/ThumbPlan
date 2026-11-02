package com.ginckl.thumbplan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    background = Background,
    surface = SurfaceCard,
    error = Danger,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = Color.White,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    background = BackgroundLight,
    surface = SurfaceCardLight,
    error = Danger,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onError = Color.White,
    surfaceVariant = SurfaceCardLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun ThumbPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Accent = Accents.Violet,
    content: @Composable () -> Unit
) {
    val targetColors = (if (darkTheme) DarkColorScheme else LightColorScheme)
        .copy(primary = accent.primary, secondary = accent.secondary)
    
    // Animate color changes for a smooth liquid transition
    val animatedPrimary by animateColorAsState(targetColors.primary, tween(500), label = "primary")
    val animatedSecondary by animateColorAsState(targetColors.secondary, tween(500), label = "secondary")
    val animatedBackground by animateColorAsState(targetColors.background, tween(500), label = "background")
    val animatedSurface by animateColorAsState(targetColors.surface, tween(500), label = "surface")
    val animatedError by animateColorAsState(targetColors.error, tween(500), label = "error")
    val animatedOnPrimary by animateColorAsState(targetColors.onPrimary, tween(500), label = "onPrimary")
    val animatedOnSecondary by animateColorAsState(targetColors.onSecondary, tween(500), label = "onSecondary")
    val animatedOnBackground by animateColorAsState(targetColors.onBackground, tween(500), label = "onBackground")
    val animatedOnSurface by animateColorAsState(targetColors.onSurface, tween(500), label = "onSurface")
    val animatedOnError by animateColorAsState(targetColors.onError, tween(500), label = "onError")
    val animatedSurfaceVariant by animateColorAsState(targetColors.surfaceVariant, tween(500), label = "surfaceVariant")
    val animatedOnSurfaceVariant by animateColorAsState(targetColors.onSurfaceVariant, tween(500), label = "onSurfaceVariant")

    val colorScheme = ColorScheme(
        primary = animatedPrimary,
        onPrimary = animatedOnPrimary,
        primaryContainer = animatedPrimary,
        onPrimaryContainer = animatedOnPrimary,
        inversePrimary = animatedPrimary,
        secondary = animatedSecondary,
        onSecondary = animatedOnSecondary,
        secondaryContainer = animatedPrimary.copy(alpha = 0.18f),
        onSecondaryContainer = animatedPrimary,
        tertiary = animatedSecondary,
        onTertiary = animatedOnSecondary,
        tertiaryContainer = animatedSecondary,
        onTertiaryContainer = animatedOnSecondary,
        background = animatedBackground,
        onBackground = animatedOnBackground,
        surface = animatedSurface,
        onSurface = animatedOnSurface,
        surfaceVariant = animatedSurfaceVariant,
        onSurfaceVariant = animatedOnSurfaceVariant,
        surfaceTint = animatedPrimary,
        inverseSurface = animatedOnBackground,
        inverseOnSurface = animatedBackground,
        error = animatedError,
        onError = animatedOnError,
        errorContainer = animatedError,
        onErrorContainer = animatedOnError,
        outline = animatedOnSurfaceVariant,
        outlineVariant = animatedOnSurfaceVariant.copy(alpha = 0.5f),
        scrim = Color.Black
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAccent provides accent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
