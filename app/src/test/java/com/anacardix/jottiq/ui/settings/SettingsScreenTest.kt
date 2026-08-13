package com.anacardix.jottiq.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.ThemePref
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val loadedState = SettingsUiState(
        isLoading = false,
        themePref = ThemePref.System,
        sortOrder = SortOrder.DateEdited,
        language = AppLanguage.System,
    )

    @Test
    fun `shows current theme, sort order, and language values`() {
        composeTestRule.setContent {
            SettingsContent(uiState = loadedState, onEvent = {}, onBackClick = {})
        }

        val activity = composeTestRule.activity
        composeTestRule.onNodeWithText(activity.getString(R.string.settings_theme_system)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.home_sort_date_edited)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.settings_language_system)).assertExists()
    }

    @Test
    fun `shows the haptics row`() {
        composeTestRule.setContent {
            SettingsContent(uiState = loadedState, onEvent = {}, onBackClick = {})
        }

        val activity = composeTestRule.activity
        composeTestRule.onNodeWithText(activity.getString(R.string.settings_haptics_row_title)).assertExists()
    }

    @Test
    fun `tapping the haptics row raises HapticsToggled with the negated value`() {
        var toggled: Boolean? = null
        composeTestRule.setContent {
            SettingsContent(
                uiState = loadedState.copy(hapticsEnabled = true),
                onEvent = { if (it is SettingsEvent.HapticsToggled) toggled = it.enabled },
                onBackClick = {},
            )
        }

        // Scrolled below the fold in this test window, so the click must scroll it into view first.
        val rowTitle = composeTestRule.activity.getString(R.string.settings_haptics_row_title)
        composeTestRule.onNodeWithText(rowTitle).performScrollTo().performClick()

        assertThat(toggled).isFalse()
    }

    @Test
    fun `tapping the Theme row raises ThemeRowClicked`() {
        var clicked = false
        composeTestRule.setContent {
            SettingsContent(
                uiState = loadedState,
                onEvent = { if (it == SettingsEvent.ThemeRowClicked) clicked = true },
                onBackClick = {},
            )
        }

        val rowTitle = composeTestRule.activity.getString(R.string.settings_theme_row_title)
        composeTestRule.onNodeWithText(rowTitle).performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `selecting Dark in the theme dialog raises ThemeSelected`() {
        var selected: ThemePref? = null
        composeTestRule.setContent {
            SettingsContent(
                uiState = loadedState.copy(activeDialog = SettingsDialog.Theme),
                onEvent = { if (it is SettingsEvent.ThemeSelected) selected = it.pref },
                onBackClick = {},
            )
        }

        val darkLabel = composeTestRule.activity.getString(R.string.settings_theme_dark)
        composeTestRule.onNodeWithText(darkLabel).performClick()

        assertThat(selected).isEqualTo(ThemePref.Dark)
    }

    @Test
    fun `selecting Title A-Z in the sort dialog raises SortOrderSelected`() {
        var selected: SortOrder? = null
        composeTestRule.setContent {
            SettingsContent(
                uiState = loadedState.copy(activeDialog = SettingsDialog.Sort),
                onEvent = { if (it is SettingsEvent.SortOrderSelected) selected = it.order },
                onBackClick = {},
            )
        }

        val titleAzLabel = composeTestRule.activity.getString(R.string.home_sort_title_az)
        composeTestRule.onNodeWithText(titleAzLabel).performClick()

        assertThat(selected).isEqualTo(SortOrder.TitleAsc)
    }

    @Test
    fun `selecting Italiano in the language dialog raises LanguageSelected`() {
        var selected: AppLanguage? = null
        composeTestRule.setContent {
            SettingsContent(
                uiState = loadedState.copy(activeDialog = SettingsDialog.Language),
                onEvent = { if (it is SettingsEvent.LanguageSelected) selected = it.language },
                onBackClick = {},
            )
        }

        val italianLabel = composeTestRule.activity.getString(R.string.settings_language_italian)
        composeTestRule.onNodeWithText(italianLabel).performClick()

        assertThat(selected).isEqualTo(AppLanguage.Italian)
    }

    @Test
    fun `selecting Portugues (Brasil) in the language dialog raises LanguageSelected`() {
        var selected: AppLanguage? = null
        composeTestRule.setContent {
            SettingsContent(
                uiState = loadedState.copy(activeDialog = SettingsDialog.Language),
                onEvent = { if (it is SettingsEvent.LanguageSelected) selected = it.language },
                onBackClick = {},
            )
        }

        val label = composeTestRule.activity.getString(R.string.settings_language_portuguese_brazil)
        composeTestRule.onNodeWithText(label).performScrollTo().performClick()

        assertThat(selected).isEqualTo(AppLanguage.PortugueseBrazil)
    }

    @Test
    fun `dialog Cancel button raises DialogDismissed`() {
        var dismissed = false
        composeTestRule.setContent {
            SettingsContent(
                uiState = loadedState.copy(activeDialog = SettingsDialog.Theme),
                onEvent = { if (it == SettingsEvent.DialogDismissed) dismissed = true },
                onBackClick = {},
            )
        }

        val cancelLabel = composeTestRule.activity.getString(R.string.settings_dialog_cancel)
        composeTestRule.onNodeWithText(cancelLabel).performClick()

        assertThat(dismissed).isTrue()
    }
}
