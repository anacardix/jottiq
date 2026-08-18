package com.anacardix.jottiq.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.height
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.domain.usecase.NoteDateGroup
import com.anacardix.jottiq.domain.usecase.RelativeDateLabel
import com.anacardix.jottiq.ui.app.LocalizedContent
import com.anacardix.jottiq.ui.common.FolderRowUi
import com.anacardix.jottiq.ui.common.NoteRowUi
import com.anacardix.jottiq.ui.common.NoteSectionUi
import com.anacardix.jottiq.ui.common.UndoAction
import com.anacardix.jottiq.ui.common.UserMessage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `placeholder text is displayed once loading completes with nothing to show`() {
        composeTestRule.setContent {
            HomeContent(uiState = HomeUiState(isLoading = false), onEvent = {})
        }

        val expectedTitle = composeTestRule.activity.getString(R.string.home_empty_title)
        val expectedSubtitle = composeTestRule.activity.getString(R.string.home_empty_subtitle)
        composeTestRule.onNodeWithText(expectedTitle).assertExists()
        composeTestRule.onNodeWithText(expectedSubtitle).assertExists()
    }

    @Test
    fun `loading indicator shows while the screen has not loaded yet`() {
        composeTestRule.setContent {
            HomeContent(uiState = HomeUiState(isLoading = true), onEvent = {})
        }

        val loadingDescription = composeTestRule.activity.getString(R.string.loading_indicator_description)
        composeTestRule.onNodeWithContentDescription(loadingDescription).assertExists()
    }

    @Test
    fun `loading indicator is gone once loading completes`() {
        composeTestRule.setContent {
            HomeContent(uiState = HomeUiState(isLoading = false), onEvent = {})
        }

        val loadingDescription = composeTestRule.activity.getString(R.string.loading_indicator_description)
        composeTestRule.onNodeWithContentDescription(loadingDescription).assertDoesNotExist()
    }

    @Test
    fun `header shows the title and item count`() {
        composeTestRule.setContent {
            HomeContent(uiState = HomeUiState(isLoading = false, itemCount = 7), onEvent = {})
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.home_title)).assertExists()
        composeTestRule.onNodeWithText("7 items").assertExists()
    }

    @Test
    fun `favorite notes render under the Favorites label`() {
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    favoriteNotes = listOf(favoriteNote(id = "1", title = "Groceries")),
                ),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.home_favorites)).assertExists()
        composeTestRule.onNodeWithText("Groceries").assertExists()
    }

    @Test
    fun `a note with no title falls back to the New Note label`() {
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    noteSections = listOf(
                        NoteSectionUi(NoteDateGroup.Today, listOf(favoriteNote(id = "1", title = ""))),
                    ),
                ),
                onEvent = {},
            )
        }

        val fallback = composeTestRule.activity.getString(R.string.untitled_note)
        composeTestRule.onNodeWithText(fallback).assertExists()
    }

    @Test
    fun `a note section renders its date header above the note`() {
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    noteSections = listOf(
                        NoteSectionUi(NoteDateGroup.Today, listOf(favoriteNote(id = "1", title = "Groceries"))),
                    ),
                ),
                onEvent = {},
            )
        }

        val todayLabel = composeTestRule.activity.getString(R.string.home_date_today)
        composeTestRule.onNodeWithText(todayLabel).assertExists()
        composeTestRule.onNodeWithText("Groceries").assertExists()
    }

    @Test
    fun `tapping a folder row raises FolderClicked`() {
        var clickedFolderId: String? = null
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    folders = listOf(FolderRowUi(id = "journal", name = "Journal", noteCount = 2, isLocked = false)),
                ),
                onEvent = { if (it is HomeEvent.FolderClicked) clickedFolderId = it.folderId },
            )
        }

        composeTestRule.onNodeWithText("Journal").performClick()

        assertThat(clickedFolderId).isEqualTo("journal")
    }

    @Test
    fun `a locked folder row shows the locked icon when the session is not unlocked`() {
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    folders = listOf(
                        FolderRowUi(id = "journal", name = "Journal", noteCount = 2, isLocked = true),
                    ),
                ),
                onEvent = {},
            )
        }

        val lockedDescription = composeTestRule.activity.getString(R.string.home_folder_locked)
        composeTestRule.onNodeWithContentDescription(lockedDescription).assertExists()
    }

    @Test
    fun `a locked folder row shows the unlocked icon once the session is unlocked`() {
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    folders = listOf(
                        FolderRowUi(
                            id = "journal",
                            name = "Journal",
                            noteCount = 2,
                            isLocked = true,
                            isSessionUnlocked = true,
                        ),
                    ),
                ),
                onEvent = {},
            )
        }

        val unlockedDescription = composeTestRule.activity.getString(R.string.home_folder_unlocked)
        composeTestRule.onNodeWithContentDescription(unlockedDescription).assertExists()
    }

    @Test
    fun `a locked note row shows the unlocked icon once the session is unlocked`() {
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    noteSections = listOf(
                        NoteSectionUi(
                            NoteDateGroup.Today,
                            listOf(
                                NoteRowUi(
                                    id = "1",
                                    title = "Gift ideas",
                                    isFavorite = false,
                                    isLocked = true,
                                    dateLabel = RelativeDateLabel.Time("14:02"),
                                    isSessionUnlocked = true,
                                ),
                            ),
                        ),
                    ),
                ),
                onEvent = {},
            )
        }

        val unlockedDescription = composeTestRule.activity.getString(R.string.home_note_unlocked)
        composeTestRule.onNodeWithContentDescription(unlockedDescription).assertExists()
    }

    @Test
    fun `expanded FAB menu shows the new folder and new note items`() {
        composeTestRule.setContent {
            HomeContent(uiState = HomeUiState(isLoading = false, isFabMenuExpanded = true), onEvent = {})
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.home_new_folder)).assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.home_new_note)).assertExists()
    }

    @Test
    fun `sort menu lists all three sort options`() {
        composeTestRule.setContent {
            HomeContent(uiState = HomeUiState(isLoading = false, isSortMenuExpanded = true), onEvent = {})
        }

        val activity = composeTestRule.activity
        composeTestRule.onNodeWithText(activity.getString(R.string.home_sort_date_edited)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.home_sort_date_created)).assertExists()
        composeTestRule.onNodeWithText(activity.getString(R.string.home_sort_title_az)).assertExists()
    }

    @Test
    fun `create-folder dialog confirm button is disabled until a name is typed`() {
        var confirmed = false
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(isLoading = false, isCreateFolderDialogVisible = true, createFolderName = ""),
                onEvent = { if (it == HomeEvent.CreateFolderConfirmed) confirmed = true },
            )
        }

        val confirmText = composeTestRule.activity.getString(R.string.home_create_folder_confirm)
        composeTestRule.onNodeWithText(confirmText).assertIsNotEnabled()
        composeTestRule.onNodeWithText(confirmText).performClick()
        assertThat(confirmed).isFalse()
    }

    @Test
    fun `typing a folder name raises CreateFolderNameChanged`() {
        var typedName: String? = null
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(isLoading = false, isCreateFolderDialogVisible = true, createFolderName = ""),
                onEvent = { if (it is HomeEvent.CreateFolderNameChanged) typedName = it.name },
            )
        }

        val fieldLabel = composeTestRule.activity.getString(R.string.home_create_folder_name_label)
        composeTestRule.onNodeWithText(fieldLabel).performTextInput("Recipes")

        assertThat(typedName).isEqualTo("Recipes")
    }

    @Test
    fun `create-folder field does not shrink once a name is typed`() {
        val name = mutableStateOf("")
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(
                    isLoading = false,
                    isCreateFolderDialogVisible = true,
                    createFolderName = name.value,
                ),
                onEvent = {},
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
                HomeContent(
                    uiState = HomeUiState(isLoading = false, isCreateFolderDialogVisible = true, createFolderName = ""),
                    onEvent = {},
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

    @Test
    fun `tapping the snackbar Undo action raises UndoDeleteClicked`() {
        val events = mutableListOf<HomeEvent>()
        val message = UserMessage(
            messageResId = R.string.item_deleted_note,
            undo = UndoAction(noteIds = listOf("note-1")),
        )
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(isLoading = false, userMessage = message),
                onEvent = { events += it },
            )
        }

        val undoLabel = composeTestRule.activity.getString(R.string.undo_action)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(undoLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(undoLabel).performClick()
        composeTestRule.waitForIdle()

        assertThat(events).contains(HomeEvent.UndoDeleteClicked(noteIds = listOf("note-1"), folderIds = emptyList()))
    }

    @Test
    fun `plus button sits above the undo snackbar when a note is deleted`() {
        val message = UserMessage(
            messageResId = R.string.item_deleted_note,
            undo = UndoAction(noteIds = listOf("note-1")),
        )
        composeTestRule.setContent {
            HomeContent(uiState = HomeUiState(isLoading = false, userMessage = message), onEvent = {})
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
        val events = mutableListOf<HomeEvent>()
        val message = UserMessage(
            messageResId = R.string.item_deleted_folder,
            undo = UndoAction(folderIds = listOf("folder-1")),
        )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HomeContent(
                uiState = HomeUiState(isLoading = false, userMessage = message),
                onEvent = { events += it },
            )
        }

        val snackbarText = composeTestRule.activity.getString(R.string.item_deleted_folder)
        composeTestRule.mainClock.advanceTimeUntil {
            composeTestRule.onAllNodesWithText(snackbarText).fetchSemanticsNodes().isNotEmpty()
        }

        // Advance well past SnackbarDuration.Short without tapping Undo or navigating away.
        composeTestRule.mainClock.advanceTimeBy(15_000)
        composeTestRule.waitForIdle()

        assertThat(events).contains(HomeEvent.UserMessageShown)
        composeTestRule.onAllNodesWithText(snackbarText).assertCountEquals(0)
    }

    @Test
    fun `empty placeholder still renders once keyed and animated`() {
        composeTestRule.setContent {
            HomeContent(uiState = HomeUiState(isLoading = false), onEvent = {})
        }

        val expectedText = composeTestRule.activity.getString(R.string.home_empty_title)
        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }

    private fun favoriteNote(id: String, title: String) = NoteRowUi(
        id = id,
        title = title,
        isFavorite = true,
        isLocked = false,
        dateLabel = RelativeDateLabel.Time("14:02"),
    )
}
