package com.anacardix.jottiq.ui.folder

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.ui.app.LocalizedContent
import com.anacardix.jottiq.ui.common.FolderRowUi
import com.anacardix.jottiq.ui.common.UndoAction
import com.anacardix.jottiq.ui.common.UserMessage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FolderScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `loading indicator shows while the screen has not loaded yet`() {
        composeTestRule.setContent {
            FolderContent(uiState = FolderUiState(isLoading = true), onEvent = {}, onBackClick = {})
        }

        val loadingDescription = composeTestRule.activity.getString(R.string.loading_indicator_description)
        composeTestRule.onNodeWithContentDescription(loadingDescription).assertExists()
    }

    @Test
    fun `loading indicator is gone once loading completes`() {
        composeTestRule.setContent {
            FolderContent(uiState = FolderUiState(isLoading = false), onEvent = {}, onBackClick = {})
        }

        val loadingDescription = composeTestRule.activity.getString(R.string.loading_indicator_description)
        composeTestRule.onNodeWithContentDescription(loadingDescription).assertDoesNotExist()
    }

    @Test
    fun `header shows the folder title and item count`() {
        composeTestRule.setContent {
            FolderContent(
                uiState = FolderUiState(
                    isLoading = false,
                    folderName = "Personal",
                    itemCount = 2,
                ),
                onEvent = {},
                onBackClick = {},
            )
        }

        composeTestRule.onNodeWithText("Personal").assertExists()
        composeTestRule.onNodeWithText("2 items").assertExists()
    }

    @Test
    fun `empty placeholder shows when the folder has no children`() {
        composeTestRule.setContent {
            FolderContent(uiState = FolderUiState(isLoading = false), onEvent = {}, onBackClick = {})
        }

        val expectedTitle = composeTestRule.activity.getString(R.string.folder_empty_title)
        val expectedSubtitle = composeTestRule.activity.getString(R.string.folder_empty_subtitle)
        composeTestRule.onNodeWithText(expectedTitle).assertExists()
        composeTestRule.onNodeWithText(expectedSubtitle).assertExists()
    }

    @Test
    fun `back button triggers onBackClick`() {
        var backClicked = false
        composeTestRule.setContent {
            FolderContent(
                uiState = FolderUiState(isLoading = false),
                onEvent = {},
                onBackClick = { backClicked = true },
            )
        }

        val description = composeTestRule.activity.getString(R.string.folder_back_action)
        composeTestRule.onNodeWithContentDescription(description).performClick()

        assertThat(backClicked).isTrue()
    }

    @Test
    fun `toolbar shows the lock action when the folder is unlocked`() {
        composeTestRule.setContent {
            FolderContent(
                uiState = FolderUiState(isLoading = false, isLocked = false),
                onEvent = {},
                onBackClick = {},
            )
        }

        val description = composeTestRule.activity.getString(R.string.folder_lock_action)
        composeTestRule.onNodeWithContentDescription(description).assertExists()
    }

    @Test
    fun `toolbar shows the remove-lock action when the folder is locked`() {
        composeTestRule.setContent {
            FolderContent(
                uiState = FolderUiState(isLoading = false, isLocked = true),
                onEvent = {},
                onBackClick = {},
            )
        }

        val description = composeTestRule.activity.getString(R.string.folder_unlock_action)
        composeTestRule.onNodeWithContentDescription(description).assertExists()
    }

    @Test
    fun `tapping the lock action raises LockToggleClicked`() {
        var toggled = false
        composeTestRule.setContent {
            FolderContent(
                uiState = FolderUiState(isLoading = false, isLocked = false),
                onEvent = { if (it is FolderEvent.LockToggleClicked) toggled = true },
                onBackClick = {},
            )
        }

        val description = composeTestRule.activity.getString(R.string.folder_lock_action)
        composeTestRule.onNodeWithContentDescription(description).performClick()

        assertThat(toggled).isTrue()
    }

    @Test
    fun `tapping a subfolder row raises FolderClicked`() {
        var clickedFolderId: String? = null
        composeTestRule.setContent {
            FolderContent(
                uiState = FolderUiState(
                    isLoading = false,
                    folders = listOf(FolderRowUi(id = "travel", name = "Travel", noteCount = 2, isLocked = false)),
                ),
                onEvent = { if (it is FolderEvent.FolderClicked) clickedFolderId = it.folderId },
                onBackClick = {},
            )
        }

        composeTestRule.onNodeWithText("Travel").performClick()

        assertThat(clickedFolderId).isEqualTo("travel")
    }

    @Test
    fun `plus button sits above the undo snackbar when a note is deleted`() {
        val message = UserMessage(
            messageResId = R.string.item_deleted_note,
            undo = UndoAction(targetId = "note-1", isFolder = false),
        )
        composeTestRule.setContent {
            FolderContent(
                uiState = FolderUiState(isLoading = false, userMessage = message),
                onEvent = {},
                onBackClick = {},
            )
        }

        val snackbarText = composeTestRule.activity.getString(R.string.item_deleted_note)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(snackbarText).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        val fabDescription = composeTestRule.activity.getString(R.string.home_toggle_fab_menu)
        val fabBottom = composeTestRule.onNodeWithContentDescription(fabDescription)
            .getUnclippedBoundsInRoot().bottom
        val snackbarTop = composeTestRule.onNodeWithText(snackbarText).getUnclippedBoundsInRoot().top

        assertThat(fabBottom.value).isLessThan(snackbarTop.value)
    }

    @Test
    fun `undo snackbar dismisses itself without user interaction`() {
        val events = mutableListOf<FolderEvent>()
        val message = UserMessage(
            messageResId = R.string.item_deleted_note,
            undo = UndoAction(targetId = "note-1", isFolder = false),
        )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            FolderContent(
                uiState = FolderUiState(isLoading = false, userMessage = message),
                onEvent = { events += it },
                onBackClick = {},
            )
        }

        val snackbarText = composeTestRule.activity.getString(R.string.item_deleted_note)
        composeTestRule.mainClock.advanceTimeUntil {
            composeTestRule.onAllNodesWithText(snackbarText).fetchSemanticsNodes().isNotEmpty()
        }

        // Advance well past SnackbarDuration.Short without tapping Undo or navigating away.
        composeTestRule.mainClock.advanceTimeBy(15_000)
        composeTestRule.waitForIdle()

        assertThat(events).contains(FolderEvent.UserMessageShown)
        composeTestRule.onAllNodesWithText(snackbarText).assertCountEquals(0)
    }

    @Test
    fun `create-folder field does not shrink once a name is typed`() {
        val name = mutableStateOf("")
        composeTestRule.setContent {
            FolderContent(
                uiState = FolderUiState(
                    isLoading = false,
                    isCreateFolderDialogVisible = true,
                    createFolderName = name.value,
                ),
                onEvent = {},
                onBackClick = {},
            )
        }

        val fieldLabel = composeTestRule.activity.getString(R.string.home_create_folder_name_label)
        val emptyHeight = composeTestRule.onNodeWithText(fieldLabel).getUnclippedBoundsInRoot().height

        name.value = "Recipes"
        composeTestRule.waitForIdle()

        val typedHeight = composeTestRule.onNodeWithText("Recipes").getUnclippedBoundsInRoot().height

        assertThat(typedHeight.value).isWithin(0.5f).of(emptyHeight.value)
    }

    @Test
    fun `create-folder field placeholder follows the in-app language override`() {
        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.Italian) {
                FolderContent(
                    uiState = FolderUiState(
                        isLoading = false,
                        isCreateFolderDialogVisible = true,
                        createFolderName = "",
                    ),
                    onEvent = {},
                    onBackClick = {},
                )
            }
        }

        // Regression test: the field's placeholder is resolved inside AlertDialog's content slot,
        // which Compose composes in a separate window that re-provides LocalContext from the real
        // Activity context — shadowing LocalizedContent's override unless the caller resolves the
        // string outside that boundary. Device/test locale here is English, so seeing the Italian
        // string confirms the override, not a device-locale fallback.
        composeTestRule.onNodeWithText("Nome della cartella").assertExists()
    }
}
