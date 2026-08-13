package com.anacardix.jottiq.ui.settings

/** One-off navigation intents raised by [SettingsViewModel], consumed once by [SettingsScreen]. */
sealed interface SettingsNavigationEvent {
    data object Back : SettingsNavigationEvent
}
