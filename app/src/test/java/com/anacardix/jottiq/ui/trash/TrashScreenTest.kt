package com.anacardix.jottiq.ui.trash

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.anacardix.jottiq.R
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrashScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val rows = listOf(
        TrashRowUi(id = "1", title = "Old shopping list", folderName = null, deletedDateText = "12 Jun", daysLeft = 6),
        TrashRowUi(id = "2", title = "", folderName = "Work", deletedDateText = "28 Jun", daysLeft = 22),
    )

    @Test
    fun `loading indicator shows while the screen has not loaded yet`() {
        composeTestRule.setContent {
            TrashContent(uiState = TrashUiState(isLoading = true), onEvent = {}, onBackClick = {})
        }

        val loadingDescription = composeTestRule.activity.getString(R.string.loading_indicator_description)
        composeTestRule.onNodeWithContentDescription(loadingDescription).assertExists()
    }

    @Test
    fun `loading indicator is gone once loading completes`() {
        composeTestRule.setContent {
            TrashContent(uiState = TrashUiState(isLoading = false), onEvent = {}, onBackClick = {})
        }

        val loadingDescription = composeTestRule.activity.getString(R.string.loading_indicator_description)
        composeTestRule.onNodeWithContentDescription(loadingDescription).assertDoesNotExist()
    }

    @Test
    fun `shows the info banner and each row's title`() {
        composeTestRule.setContent {
            TrashContent(uiState = TrashUiState(isLoading = false, items = rows), onEvent = {}, onBackClick = {})
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.trash_info_banner)).assertExists()
        composeTestRule.onNodeWithText("Old shopping list").assertExists()
    }

    @Test
    fun `a blank title falls back to Untitled note`() {
        composeTestRule.setContent {
            TrashContent(uiState = TrashUiState(isLoading = false, items = rows), onEvent = {}, onBackClick = {})
        }

        val untitled = composeTestRule.activity.getString(R.string.untitled_note)
        composeTestRule.onNodeWithText(untitled).assertExists()
    }

    @Test
    fun `entering selection mode does not shift the list down`() {
        val baseState = TrashUiState(isLoading = false, items = rows)
        val state = mutableStateOf(baseState)
        composeTestRule.setContent {
            TrashContent(uiState = state.value, onEvent = {}, onBackClick = {})
        }

        val topBeforeSelection = composeTestRule.onNodeWithText("Old shopping list")
            .getUnclippedBoundsInRoot().top

        // Same hazard as HomeScreenTest's equivalent test: a wide selection actions row used to
        // wrap the app bar's "1 selected" title and push this row down — see JottiqTopAppBar's kdoc.
        state.value = baseState.copy(selectionMode = true, selectedNoteIds = setOf("1"))
        composeTestRule.waitForIdle()

        val topAfterSelection = composeTestRule.onNodeWithText("Old shopping list")
            .getUnclippedBoundsInRoot().top

        assertThat(topAfterSelection.value).isWithin(0.5f).of(topBeforeSelection.value)
    }

    @Test
    fun `row subtitle composes folder, date, and days-left`() {
        composeTestRule.setContent {
            TrashContent(uiState = TrashUiState(isLoading = false, items = rows), onEvent = {}, onBackClick = {})
        }

        composeTestRule.onNodeWithText("In Notes · Deleted 12 Jun · 6 days left").assertExists()
        composeTestRule.onNodeWithText("In Work · Deleted 28 Jun · 22 days left").assertExists()
    }

    @Test
    fun `tapping restore raises RestoreClicked for that row`() {
        var restoredId: String? = null
        composeTestRule.setContent {
            TrashContent(
                uiState = TrashUiState(isLoading = false, items = rows.take(1)),
                onEvent = { if (it is TrashEvent.RestoreClicked) restoredId = it.id },
                onBackClick = {},
            )
        }

        val restoreDescription = composeTestRule.activity.getString(R.string.trash_restore_action)
        composeTestRule.onNodeWithContentDescription(restoreDescription).performClick()

        assertThat(restoredId).isEqualTo("1")
    }

    @Test
    fun `tapping Empty trash opens the confirmation dialog`() {
        var clicked = false
        composeTestRule.setContent {
            TrashContent(
                uiState = TrashUiState(isLoading = false, items = rows),
                onEvent = { if (it == TrashEvent.EmptyTrashClicked) clicked = true },
                onBackClick = {},
            )
        }

        val emptyText = composeTestRule.activity.getString(R.string.trash_empty_action)
        composeTestRule.onNodeWithText(emptyText).performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `empty-trash dialog confirm raises EmptyTrashConfirmed`() {
        var confirmed = false
        composeTestRule.setContent {
            TrashContent(
                uiState = TrashUiState(isLoading = false, items = rows, isEmptyTrashDialogVisible = true),
                onEvent = { if (it == TrashEvent.EmptyTrashConfirmed) confirmed = true },
                onBackClick = {},
            )
        }

        // The dialog's confirm button intentionally shares its label with the app-bar trigger
        // button (both are visible while the dialog is open), so disambiguate by position.
        val confirmText = composeTestRule.activity.getString(R.string.trash_empty_dialog_confirm)
        composeTestRule.onAllNodesWithText(confirmText).onLast().performClick()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun `Empty trash button is disabled when there is nothing to empty`() {
        composeTestRule.setContent {
            TrashContent(
                uiState = TrashUiState(isLoading = false, items = emptyList()),
                onEvent = {},
                onBackClick = {},
            )
        }

        val emptyText = composeTestRule.activity.getString(R.string.trash_empty_action)
        composeTestRule.onNodeWithText(emptyText).assertIsNotEnabled()
    }

    @Test
    fun `shows the empty-state message when trash is empty`() {
        composeTestRule.setContent {
            TrashContent(
                uiState = TrashUiState(isLoading = false, items = emptyList()),
                onEvent = {},
                onBackClick = {},
            )
        }

        val placeholder = composeTestRule.activity.getString(R.string.trash_empty_placeholder)
        composeTestRule.onNodeWithText(placeholder).assertExists()
    }
}
