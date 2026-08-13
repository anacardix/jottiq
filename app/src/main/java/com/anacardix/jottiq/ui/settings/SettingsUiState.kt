package com.anacardix.jottiq.ui.settings

import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.ThemePref

/** UI state for [SettingsScreen], modeled on `design/15. Settings.png`. */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val themePref: ThemePref = ThemePref.System,
    val sortOrder: SortOrder = SortOrder.DateEdited,
    val language: AppLanguage = AppLanguage.System,
    val hapticsEnabled: Boolean = true,
    val activeDialog: SettingsDialog? = null,
)

/** Which single-choice picker (if any) is currently shown as an [androidx.compose.material3.AlertDialog]. */
enum class SettingsDialog {
    Theme,
    Sort,
    Language,
}
