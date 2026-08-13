// Hex color literals are the tokens themselves — the constant name (e.g. Primary) already
// documents them, so a "magic number" warning per literal adds no value here.
@file:Suppress("MagicNumber")

package com.anacardix.jottiq.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Static fallback M3 color roles for light theme, resolved from seed `#D97757` (the app-icon
 * clay) via the HCT tonal-palette algorithm — see `design/design-tokens.png`. Roles that appear
 * in the approved mockups are pinned to the documented hex exactly; roles the mockups never
 * exercise (tertiary family, container/dim/bright variants, etc.) are derived from the same seed
 * (Fidelity variant, which is what reproduces the mockup's brand-exact `primaryContainer`) so the
 * whole scheme stays internally consistent. Pinned hexes must not be re-tuned by eye against any
 * single display (emulator, OLED, LCD panels all render differently) — `design/design-tokens.png`
 * is the sole source of truth. Dynamic color (Android 12+) overrides all of this at runtime when
 * the user selects System — see [JottiqTheme].
 */
internal object JottiqLightColors {
    val Primary = Color(0xFF9E4A2E)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFD97757)
    val OnPrimaryContainer = Color(0xFFFFF3EC)
    val InversePrimary = Color(0xFFFFB59E)
    val Secondary = Color(0xFF77574B)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFF5DFD4)
    val OnSecondaryContainer = Color(0xFF7B4F41)
    val Tertiary = Color(0xFF006B5F)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFF09A493)
    val OnTertiaryContainer = Color(0xFF00312B)
    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)
    val Background = Color(0xFFFCF8F4)
    val OnBackground = Color(0xFF221A16)
    val Surface = Color(0xFFFCF8F4)
    val OnSurface = Color(0xFF221A16)
    val SurfaceVariant = Color(0xFFF8DDD5)
    val OnSurfaceVariant = Color(0xFF53433C)
    val Outline = Color(0xFF85736B)
    val OutlineVariant = Color(0xFFDBC1B9)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF7F1EB)
    val SurfaceContainer = Color(0xFFF2EBE4)
    val SurfaceContainerHigh = Color(0xFFECE5DE)
    val SurfaceContainerHighest = Color(0xFFE6DFD8)
    val SurfaceDim = Color(0xFFE8D6D2)
    val SurfaceBright = Color(0xFFFCF8F4)
    val InverseSurface = Color(0xFF382E29)
    val InverseOnSurface = Color(0xFFFFEDE8)
    val Scrim = Color(0xFF000000)
    val SurfaceTint = Primary
}

/**
 * Fixed, theme-independent accent colors for the two note-row swipe actions. Deliberately outside
 * the M3 color scheme: [JottiqLightColors.ErrorContainer]/[JottiqLightColors.TertiaryContainer] (and
 * their dynamic-color equivalents on Android 12+) are pastel container tones, but delete/favorite
 * need to read unambiguously as bright red/green regardless of theme or wallpaper.
 */
internal object SwipeActionColors {
    val Delete = Color(0xFFBA1A1A)
    val Favorite = Color(0xFFD97757)
    val OnAction = Color(0xFFFFFFFF)
}

/** Static fallback M3 color roles for dark theme, same seed and algorithm as [JottiqLightColors]. */
internal object JottiqDarkColors {
    val Primary = Color(0xFFFFB59E)
    val OnPrimary = Color(0xFF5A1B04)
    val PrimaryContainer = Color(0xFFC0603F)
    val OnPrimaryContainer = Color(0xFFFFF0E8)
    val InversePrimary = Color(0xFF9E4A2E)
    val Secondary = Color(0xFFE7BFAF)
    val OnSecondary = Color(0xFF4B271B)
    val SecondaryContainer = Color(0xFF5D4034)
    val OnSecondaryContainer = Color(0xFFE1A897)
    val Tertiary = Color(0xFF5EDAC7)
    val OnTertiary = Color(0xFF003731)
    val TertiaryContainer = Color(0xFF09A493)
    val OnTertiaryContainer = Color(0xFF00312B)
    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)
    val Background = Color(0xFF191411)
    val OnBackground = Color(0xFFEFE0DA)
    val Surface = Color(0xFF191411)
    val OnSurface = Color(0xFFEFE0DA)
    val SurfaceVariant = Color(0xFF55433D)
    val OnSurfaceVariant = Color(0xFFD8C3B9)
    val Outline = Color(0xFFA08D84)
    val OutlineVariant = Color(0xFF55433D)
    val SurfaceContainerLowest = Color(0xFF140C0A)
    val SurfaceContainerLow = Color(0xFF221A16)
    val SurfaceContainer = Color(0xFF261E1A)
    val SurfaceContainerHigh = Color(0xFF312824)
    val SurfaceContainerHighest = Color(0xFF3C332E)
    val SurfaceDim = Color(0xFF191411)
    val SurfaceBright = Color(0xFF413734)
    val InverseSurface = Color(0xFFEFE0DA)
    val InverseOnSurface = Color(0xFF382E2B)
    val Scrim = Color(0xFF000000)
    val SurfaceTint = Primary
}
