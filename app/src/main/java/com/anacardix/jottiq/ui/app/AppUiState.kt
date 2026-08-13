package com.anacardix.jottiq.ui.app

import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.ThemePref

/** App-shell state read by [AppRoot] to drive the root theme and locale — not a nav screen. */
data class AppUiState(
    val themePref: ThemePref = ThemePref.System,
    val language: AppLanguage = AppLanguage.System,
    val hapticsEnabled: Boolean = true,
)
