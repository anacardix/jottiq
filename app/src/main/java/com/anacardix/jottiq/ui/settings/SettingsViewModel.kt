package com.anacardix.jottiq.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.SettingsRepository
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.ThemePref
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val navigationChannel = Channel<SettingsNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<SettingsNavigationEvent> = navigationChannel.receiveAsFlow()

    private var hasStartedObserving = false

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.ScreenShown -> startObservingIfNeeded()
            SettingsEvent.BackClicked -> navigationChannel.trySend(SettingsNavigationEvent.Back)
            SettingsEvent.ThemeRowClicked -> openDialog(SettingsDialog.Theme)
            SettingsEvent.SortRowClicked -> openDialog(SettingsDialog.Sort)
            SettingsEvent.LanguageRowClicked -> openDialog(SettingsDialog.Language)
            SettingsEvent.DialogDismissed -> _uiState.update { it.copy(activeDialog = null) }
            is SettingsEvent.ThemeSelected -> onThemeSelected(event.pref)
            is SettingsEvent.SortOrderSelected -> onSortOrderSelected(event.order)
            is SettingsEvent.LanguageSelected -> onLanguageSelected(event.language)
            is SettingsEvent.HapticsToggled -> onHapticsToggled(event.enabled)
        }
    }

    private fun startObservingIfNeeded() {
        if (hasStartedObserving) return
        hasStartedObserving = true
        combine(
            settingsRepository.observeThemePref(),
            settingsRepository.observeSortOrder(),
            settingsRepository.observeLanguage(),
            settingsRepository.observeHapticsEnabled(),
        ) { themePref, sortOrder, language, hapticsEnabled ->
            SettingsSnapshot(themePref, sortOrder, language, hapticsEnabled)
        }
            .onEach { snapshot ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        themePref = snapshot.themePref,
                        sortOrder = snapshot.sortOrder,
                        language = snapshot.language,
                        hapticsEnabled = snapshot.hapticsEnabled,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private data class SettingsSnapshot(
        val themePref: ThemePref,
        val sortOrder: SortOrder,
        val language: AppLanguage,
        val hapticsEnabled: Boolean,
    )

    private fun openDialog(dialog: SettingsDialog) {
        _uiState.update { it.copy(activeDialog = dialog) }
    }

    private fun onThemeSelected(pref: ThemePref) {
        _uiState.update { it.copy(activeDialog = null) }
        viewModelScope.launch { settingsRepository.setThemePref(pref) }
    }

    private fun onSortOrderSelected(order: SortOrder) {
        _uiState.update { it.copy(activeDialog = null) }
        viewModelScope.launch { settingsRepository.setSortOrder(order) }
    }

    private fun onLanguageSelected(language: AppLanguage) {
        _uiState.update { it.copy(activeDialog = null) }
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    private fun onHapticsToggled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    }
}
