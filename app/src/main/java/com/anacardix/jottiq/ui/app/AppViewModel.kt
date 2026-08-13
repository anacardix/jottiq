package com.anacardix.jottiq.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anacardix.jottiq.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Root-level, read-only view of the settings that must be applied above [com.anacardix.jottiq.ui.navigation.JottiqNavHost]
 * (theme + locale). Unlike screen ViewModels this has no `onEvent` — changes are made from the
 * Settings screen's own ViewModel and observed here via the shared [SettingsRepository].
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        combine(
            settingsRepository.observeThemePref(),
            settingsRepository.observeLanguage(),
            settingsRepository.observeHapticsEnabled(),
        ) { themePref, language, hapticsEnabled -> AppUiState(themePref, language, hapticsEnabled) }
            .onEach { snapshot -> _uiState.value = snapshot }
            .launchIn(viewModelScope)
    }
}
