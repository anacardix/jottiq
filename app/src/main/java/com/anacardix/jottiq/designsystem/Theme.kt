package com.anacardix.jottiq.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Static fallback schemes: full M3 role set resolved from seed #D97757 — see [JottiqLightColors]
// and [JottiqDarkColors] for provenance. Overridden by dynamic color on Android 12+.
private val DarkColorScheme = darkColorScheme(
    primary = JottiqDarkColors.Primary,
    onPrimary = JottiqDarkColors.OnPrimary,
    primaryContainer = JottiqDarkColors.PrimaryContainer,
    onPrimaryContainer = JottiqDarkColors.OnPrimaryContainer,
    inversePrimary = JottiqDarkColors.InversePrimary,
    secondary = JottiqDarkColors.Secondary,
    onSecondary = JottiqDarkColors.OnSecondary,
    secondaryContainer = JottiqDarkColors.SecondaryContainer,
    onSecondaryContainer = JottiqDarkColors.OnSecondaryContainer,
    tertiary = JottiqDarkColors.Tertiary,
    onTertiary = JottiqDarkColors.OnTertiary,
    tertiaryContainer = JottiqDarkColors.TertiaryContainer,
    onTertiaryContainer = JottiqDarkColors.OnTertiaryContainer,
    error = JottiqDarkColors.Error,
    onError = JottiqDarkColors.OnError,
    errorContainer = JottiqDarkColors.ErrorContainer,
    onErrorContainer = JottiqDarkColors.OnErrorContainer,
    background = JottiqDarkColors.Background,
    onBackground = JottiqDarkColors.OnBackground,
    surface = JottiqDarkColors.Surface,
    onSurface = JottiqDarkColors.OnSurface,
    surfaceVariant = JottiqDarkColors.SurfaceVariant,
    onSurfaceVariant = JottiqDarkColors.OnSurfaceVariant,
    outline = JottiqDarkColors.Outline,
    outlineVariant = JottiqDarkColors.OutlineVariant,
    surfaceContainerLowest = JottiqDarkColors.SurfaceContainerLowest,
    surfaceContainerLow = JottiqDarkColors.SurfaceContainerLow,
    surfaceContainer = JottiqDarkColors.SurfaceContainer,
    surfaceContainerHigh = JottiqDarkColors.SurfaceContainerHigh,
    surfaceContainerHighest = JottiqDarkColors.SurfaceContainerHighest,
    surfaceDim = JottiqDarkColors.SurfaceDim,
    surfaceBright = JottiqDarkColors.SurfaceBright,
    inverseSurface = JottiqDarkColors.InverseSurface,
    inverseOnSurface = JottiqDarkColors.InverseOnSurface,
    scrim = JottiqDarkColors.Scrim,
    surfaceTint = JottiqDarkColors.SurfaceTint,
)

private val LightColorScheme = lightColorScheme(
    primary = JottiqLightColors.Primary,
    onPrimary = JottiqLightColors.OnPrimary,
    primaryContainer = JottiqLightColors.PrimaryContainer,
    onPrimaryContainer = JottiqLightColors.OnPrimaryContainer,
    inversePrimary = JottiqLightColors.InversePrimary,
    secondary = JottiqLightColors.Secondary,
    onSecondary = JottiqLightColors.OnSecondary,
    secondaryContainer = JottiqLightColors.SecondaryContainer,
    onSecondaryContainer = JottiqLightColors.OnSecondaryContainer,
    tertiary = JottiqLightColors.Tertiary,
    onTertiary = JottiqLightColors.OnTertiary,
    tertiaryContainer = JottiqLightColors.TertiaryContainer,
    onTertiaryContainer = JottiqLightColors.OnTertiaryContainer,
    error = JottiqLightColors.Error,
    onError = JottiqLightColors.OnError,
    errorContainer = JottiqLightColors.ErrorContainer,
    onErrorContainer = JottiqLightColors.OnErrorContainer,
    background = JottiqLightColors.Background,
    onBackground = JottiqLightColors.OnBackground,
    surface = JottiqLightColors.Surface,
    onSurface = JottiqLightColors.OnSurface,
    surfaceVariant = JottiqLightColors.SurfaceVariant,
    onSurfaceVariant = JottiqLightColors.OnSurfaceVariant,
    outline = JottiqLightColors.Outline,
    outlineVariant = JottiqLightColors.OutlineVariant,
    surfaceContainerLowest = JottiqLightColors.SurfaceContainerLowest,
    surfaceContainerLow = JottiqLightColors.SurfaceContainerLow,
    surfaceContainer = JottiqLightColors.SurfaceContainer,
    surfaceContainerHigh = JottiqLightColors.SurfaceContainerHigh,
    surfaceContainerHighest = JottiqLightColors.SurfaceContainerHighest,
    surfaceDim = JottiqLightColors.SurfaceDim,
    surfaceBright = JottiqLightColors.SurfaceBright,
    inverseSurface = JottiqLightColors.InverseSurface,
    inverseOnSurface = JottiqLightColors.InverseOnSurface,
    scrim = JottiqLightColors.Scrim,
    surfaceTint = JottiqLightColors.SurfaceTint,
)

/**
 * App-wide theme built on Material 3 Expressive. Keep all `ExperimentalMaterial3ExpressiveApi`
 * usage confined to this package so the future upgrade to a stable Material3 release touches
 * only [designsystem].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JottiqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // `enableEdgeToEdge()` only sets status/nav bar icon appearance once, from the system's dark
    // mode config at Activity creation. [darkTheme] here can be overridden by ThemePref
    // independently of the system setting, so without this the icons can end up light-on-light
    // or dark-on-dark whenever the in-app choice and the system setting disagree.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = JottiqTypography,
        shapes = JottiqShapes,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
