package com.anacardix.jottiq.ui.settings

import app.cash.turbine.test
import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.ThemePref
import com.anacardix.jottiq.fakes.FakeSettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository()

    private fun viewModel() = SettingsViewModel(settingsRepository)

    @Test
    fun `initial state is loading`() = runTest {
        assertThat(viewModel().uiState.value.isLoading).isTrue()
    }

    @Test
    fun `ScreenShown loads the current theme, sort order, language, and haptics setting`() = runTest {
        settingsRepository.setThemePref(ThemePref.Dark)
        settingsRepository.setSortOrder(SortOrder.TitleAsc)
        settingsRepository.setLanguage(AppLanguage.Italian)
        settingsRepository.setHapticsEnabled(false)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()

            viewModel.onEvent(SettingsEvent.ScreenShown)

            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.themePref).isEqualTo(ThemePref.Dark)
            assertThat(loaded.sortOrder).isEqualTo(SortOrder.TitleAsc)
            assertThat(loaded.language).isEqualTo(AppLanguage.Italian)
            assertThat(loaded.hapticsEnabled).isFalse()
        }
    }

    @Test
    fun `ThemeRowClicked opens the theme dialog and DialogDismissed closes it unchanged`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SettingsEvent.ScreenShown)

        viewModel.onEvent(SettingsEvent.ThemeRowClicked)
        assertThat(viewModel.uiState.value.activeDialog).isEqualTo(SettingsDialog.Theme)

        viewModel.onEvent(SettingsEvent.DialogDismissed)
        assertThat(viewModel.uiState.value.activeDialog).isNull()
        assertThat(viewModel.uiState.value.themePref).isEqualTo(ThemePref.System)
    }

    @Test
    fun `ThemeSelected persists the choice and closes the dialog`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SettingsEvent.ScreenShown)
        viewModel.onEvent(SettingsEvent.ThemeRowClicked)

        viewModel.onEvent(SettingsEvent.ThemeSelected(ThemePref.Dark))

        assertThat(viewModel.uiState.value.activeDialog).isNull()
        assertThat(viewModel.uiState.value.themePref).isEqualTo(ThemePref.Dark)
        assertThat(settingsRepository.observeThemePref().value).isEqualTo(ThemePref.Dark)
    }

    @Test
    fun `SortRowClicked opens the sort dialog and SortOrderSelected persists the choice`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SettingsEvent.ScreenShown)

        viewModel.onEvent(SettingsEvent.SortRowClicked)
        assertThat(viewModel.uiState.value.activeDialog).isEqualTo(SettingsDialog.Sort)

        viewModel.onEvent(SettingsEvent.SortOrderSelected(SortOrder.DateCreated))

        assertThat(viewModel.uiState.value.activeDialog).isNull()
        assertThat(viewModel.uiState.value.sortOrder).isEqualTo(SortOrder.DateCreated)
        assertThat(settingsRepository.observeSortOrder().value).isEqualTo(SortOrder.DateCreated)
    }

    @Test
    fun `LanguageRowClicked opens the language dialog and LanguageSelected persists the choice`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SettingsEvent.ScreenShown)

        viewModel.onEvent(SettingsEvent.LanguageRowClicked)
        assertThat(viewModel.uiState.value.activeDialog).isEqualTo(SettingsDialog.Language)

        viewModel.onEvent(SettingsEvent.LanguageSelected(AppLanguage.Italian))

        assertThat(viewModel.uiState.value.activeDialog).isNull()
        assertThat(viewModel.uiState.value.language).isEqualTo(AppLanguage.Italian)
        assertThat(settingsRepository.observeLanguage().value).isEqualTo(AppLanguage.Italian)
    }

    @Test
    fun `HapticsToggled persists the new value and updates state`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SettingsEvent.ScreenShown)

        viewModel.onEvent(SettingsEvent.HapticsToggled(false))

        assertThat(settingsRepository.observeHapticsEnabled().value).isFalse()
    }

    @Test
    fun `BackClicked emits the Back navigation event`() = runTest {
        val viewModel = viewModel()

        viewModel.navigationEvents.test {
            viewModel.onEvent(SettingsEvent.BackClicked)

            assertThat(awaitItem()).isEqualTo(SettingsNavigationEvent.Back)
        }
    }
}
