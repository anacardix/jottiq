package com.anacardix.jottiq.ui.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anacardix.jottiq.designsystem.JottiqTheme
import com.anacardix.jottiq.designsystem.LocalHapticsEnabled
import com.anacardix.jottiq.domain.ThemePref
import com.anacardix.jottiq.ui.navigation.JottiqNavHost

/** App shell: applies the persisted theme + language + haptics preferences, then hosts [JottiqNavHost]. */
@Composable
fun AppRoot(viewModel: AppViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LocalizedContent(language = uiState.language) {
        val darkTheme = when (uiState.themePref) {
            ThemePref.System -> isSystemInDarkTheme()
            ThemePref.Light -> false
            ThemePref.Dark -> true
        }
        JottiqTheme(darkTheme = darkTheme, dynamicColor = resolveDynamicColor(uiState.themePref)) {
            CompositionLocalProvider(LocalHapticsEnabled provides uiState.hapticsEnabled) {
                JottiqNavHost()
            }
        }
    }
}

/**
 * Wallpaper-derived Material You color only applies when the user leaves the theme on System —
 * an explicit Light/Dark choice always resolves to Jottiq's own design tokens (see [JottiqTheme]).
 */
internal fun resolveDynamicColor(themePref: ThemePref): Boolean = themePref == ThemePref.System
