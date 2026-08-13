package com.anacardix.jottiq.ui.app

import app.cash.turbine.test
import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.ThemePref
import com.anacardix.jottiq.fakes.FakeSettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository()

    @Test
    fun `initial state defaults to System theme and System language`() = runTest {
        val viewModel = AppViewModel(settingsRepository)

        assertThat(viewModel.uiState.value).isEqualTo(AppUiState(ThemePref.System, AppLanguage.System))
    }

    @Test
    fun `uiState reflects theme and language changes from the repository`() = runTest {
        val viewModel = AppViewModel(settingsRepository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(AppUiState(ThemePref.System, AppLanguage.System))

            settingsRepository.setThemePref(ThemePref.Dark)
            assertThat(awaitItem()).isEqualTo(AppUiState(ThemePref.Dark, AppLanguage.System))

            settingsRepository.setLanguage(AppLanguage.Italian)
            assertThat(awaitItem()).isEqualTo(AppUiState(ThemePref.Dark, AppLanguage.Italian))

            settingsRepository.setHapticsEnabled(false)
            assertThat(awaitItem()).isEqualTo(AppUiState(ThemePref.Dark, AppLanguage.Italian, hapticsEnabled = false))
        }
    }
}
