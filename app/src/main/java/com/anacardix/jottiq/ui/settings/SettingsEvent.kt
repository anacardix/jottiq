package com.anacardix.jottiq.ui.settings

import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.ThemePref

/** Intents for [SettingsScreen]. */
sealed interface SettingsEvent {
    data object ScreenShown : SettingsEvent
    data object BackClicked : SettingsEvent
    data object ThemeRowClicked : SettingsEvent
    data object SortRowClicked : SettingsEvent
    data object LanguageRowClicked : SettingsEvent
    data object DialogDismissed : SettingsEvent
    data class ThemeSelected(val pref: ThemePref) : SettingsEvent
    data class SortOrderSelected(val order: SortOrder) : SettingsEvent
    data class LanguageSelected(val language: AppLanguage) : SettingsEvent
    data class HapticsToggled(val enabled: Boolean) : SettingsEvent
}
