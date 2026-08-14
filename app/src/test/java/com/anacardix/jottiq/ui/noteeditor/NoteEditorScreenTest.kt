package com.anacardix.jottiq.ui.noteeditor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.height
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.AppLanguage
import com.anacardix.jottiq.ui.app.LocalizedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoteEditorScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun baseState(isEditing: Boolean = false) = NoteEditorUiState(
        isLoading = false,
        title = "Groceries",
        isEditing = isEditing,
        dateLabel = "14:02",
        wasEdited = true,
        segments = listOf(
            EditorSegment.Rich(id = "p1", state = newRichTextState("<p>Milk</p>")),
        ),
    )

    @Test
    fun `loading indicator shows while the note has not loaded yet`() {
        composeTestRule.setContent {
            NoteEditorContent(uiState = NoteEditorUiState(isLoading = true), onEvent = {})
        }

        val loadingDescription = composeTestRule.activity.getString(R.string.loading_indicator_description)
        composeTestRule.onNodeWithContentDescription(loadingDescription).assertExists()
    }

    @Test
    fun `loading indicator is gone once loading completes`() {
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState(), onEvent = {})
        }

        val loadingDescription = composeTestRule.activity.getString(R.string.loading_indicator_description)
        composeTestRule.onNodeWithContentDescription(loadingDescription).assertDoesNotExist()
    }

    @Test
    fun `read mode shows the title and edited subtitle`() {
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState(), onEvent = {})
        }

        composeTestRule.onNodeWithText("Groceries").assertExists()
        composeTestRule.onNodeWithText("Edited 14:02").assertExists()
    }

    @Test
    fun `read mode shows the Created subtitle when the note has not been edited yet`() {
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState().copy(wasEdited = false), onEvent = {})
        }

        composeTestRule.onNodeWithText("Created 14:02").assertExists()
    }

    @Test
    fun `edit mode shows the New Note hint when the title is blank`() {
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState(isEditing = true).copy(title = ""), onEvent = {})
        }

        val defaultTitle = composeTestRule.activity.getString(R.string.untitled_note)
        composeTestRule.onNodeWithText(defaultTitle).assertExists()
    }

    @Test
    fun `typing into a blank title does not need to clear the New Note hint first`() {
        var changedTitle: String? = null
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState(isEditing = true).copy(title = ""),
                onEvent = { if (it is NoteEditorEvent.TitleChanged) changedTitle = it.title },
            )
        }

        composeTestRule.onNodeWithTag(NOTE_TITLE_FIELD_TAG).performTextInput("Groceries")

        assertThat(changedTitle).isEqualTo("Groceries")
    }

    @Test
    fun `favorite subtitle appends when the note is a favorite`() {
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState().copy(isFavorite = true), onEvent = {})
        }

        composeTestRule.onNodeWithText("Edited 14:02 · Favorite").assertExists()
    }

    @Test
    fun `edit mode keeps the read-mode top bar actions visible`() {
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState(isEditing = true), onEvent = {})
        }

        val favoriteDescription = composeTestRule.activity.getString(R.string.note_editor_favorite_action)
        val lockDescription = composeTestRule.activity.getString(R.string.note_editor_lock_action)
        val moveDescription = composeTestRule.activity.getString(R.string.note_editor_move_action)
        val deleteDescription = composeTestRule.activity.getString(R.string.note_editor_delete_action)
        composeTestRule.onNodeWithContentDescription(favoriteDescription).assertExists()
        composeTestRule.onNodeWithContentDescription(lockDescription).assertExists()
        composeTestRule.onNodeWithContentDescription(moveDescription).assertExists()
        composeTestRule.onNodeWithContentDescription(deleteDescription).assertExists()
    }

    @Test
    fun `subtitle stays in the same position between read and edit mode`() {
        // useUnmergedTree: read mode's "tap anywhere to edit" clickable spans the whole
        // (full-size) scrolling column, which merges the subtitle's semantics into that
        // column's node and would report the column's position instead of the subtitle's own —
        // an unrelated test-measurement artifact, not a rendering difference. The unmerged tree
        // reports the subtitle's actual drawn position in both modes.
        val editing = mutableStateOf(false)
        val base = baseState()
        composeTestRule.setContent {
            NoteEditorContent(uiState = base.copy(isEditing = editing.value), onEvent = {})
        }

        val readTop = composeTestRule.onNodeWithText("Edited 14:02", useUnmergedTree = true)
            .fetchSemanticsNode().positionInRoot.y

        editing.value = true
        composeTestRule.waitForIdle()

        val editTop = composeTestRule.onNodeWithText("Edited 14:02", useUnmergedTree = true)
            .fetchSemanticsNode().positionInRoot.y

        // Sub-pixel tolerance guards against font-metric rounding; the bug was a multi-dp jump.
        assertThat(editTop).isWithin(1.0f).of(readTop)
    }

    @Test
    fun `tapping the title in read mode requests edit mode`() {
        var requested = false
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState(),
                onEvent = { if (it is NoteEditorEvent.EditModeRequested) requested = true },
            )
        }

        composeTestRule.onNodeWithText("Groceries").performClick()

        assertThat(requested).isTrue()
    }

    @Test
    fun `tapping read-mode body text requests edit mode at that segment`() {
        var requestedSegmentId: String? = null
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState(),
                onEvent = { if (it is NoteEditorEvent.EditModeRequested) requestedSegmentId = it.segmentId },
            )
        }

        composeTestRule.onNodeWithText("Milk").performClick()

        assertThat(requestedSegmentId).isEqualTo("p1")
    }

    @Test
    fun `tapping the empty body area in edit mode requests focus on the last segment`() {
        var received: NoteEditorEvent.EditModeRequested? = null
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState(isEditing = true),
                onEvent = { if (it is NoteEditorEvent.EditModeRequested) received = it },
            )
        }

        composeTestRule.onNodeWithTag(NOTE_BODY_TAG).performClick()

        assertThat(received).isNotNull()
        assertThat(received?.segmentId).isNull()
    }

    @Test
    fun `read mode renders rich segment text`() {
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState().copy(
                    segments = listOf(
                        EditorSegment.Rich(id = "p1", state = newRichTextState("<p>Farmers <b>market</b></p>")),
                    ),
                ),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText("Farmers market").assertExists()
    }

    // Regression coverage for the "clicking the row opens the link" bug: a paragraph that's just a
    // short link still spans the full width of its read-mode row, so a tap in the row's blank
    // trailing space (here, performClick()'s default center-of-node tap, well past a 4-character
    // link in a full-width row) must start editing, not open the link — see
    // NoteReadText.kt's interceptDeadZoneTaps.
    @Test
    fun `tapping past the end of a link-only line in read mode requests edit mode, not the link`() {
        var requestedSegmentId: String? = null
        var openedUrl: String? = null
        val fakeUriHandler = FakeUriHandler { openedUrl = it }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides fakeUriHandler) {
                NoteEditorContent(
                    uiState = baseState().copy(
                        segments = listOf(
                            EditorSegment.Rich(
                                id = "p1",
                                state = newRichTextState("<p><a href=\"https://example.com\">Link</a></p>"),
                            ),
                        ),
                    ),
                    onEvent = { if (it is NoteEditorEvent.EditModeRequested) requestedSegmentId = it.segmentId },
                )
            }
        }

        composeTestRule.onNodeWithText("Link").performClick()

        assertThat(requestedSegmentId).isEqualTo("p1")
        assertThat(openedUrl).isNull()
    }

    // Companion to the dead-zone regression above: tapping the link's own glyphs must still open it.
    @Test
    fun `tapping the link glyphs themselves in read mode opens the link`() {
        var requestedSegmentId: String? = null
        var openedUrl: String? = null
        val fakeUriHandler = FakeUriHandler { openedUrl = it }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides fakeUriHandler) {
                NoteEditorContent(
                    uiState = baseState().copy(
                        segments = listOf(
                            EditorSegment.Rich(
                                id = "p1",
                                state = newRichTextState("<p><a href=\"https://example.com\">Link</a></p>"),
                            ),
                        ),
                    ),
                    onEvent = { if (it is NoteEditorEvent.EditModeRequested) requestedSegmentId = it.segmentId },
                )
            }
        }

        composeTestRule.onNodeWithText("Link").performTouchInput { click(Offset(1f, center.y)) }

        assertThat(openedUrl).isEqualTo("https://example.com")
        assertThat(requestedSegmentId).isNull()
    }

    // Regression coverage for the read/edit heading-size bug: read mode now renders headings via
    // the library's own BasicRichText against the same RichTextState edit mode uses, rather than a
    // hand-rolled style computed from the domain model, so this exercises that path renders at all.
    @Test
    fun `read mode renders a heading segment`() {
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState().copy(
                    segments = listOf(
                        EditorSegment.Rich(id = "p1", state = newRichTextState("<h1>Groceries list</h1>")),
                    ),
                ),
                onEvent = {},
            )
        }

        composeTestRule.onNodeWithText("Groceries list").assertExists()
    }

    @Test
    fun `insert-link dialog's Insert button is disabled until a url is provided`() {
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState(isEditing = true).copy(isLinkDialogVisible = true, linkUrl = ""),
                onEvent = {},
            )
        }

        val insertText = composeTestRule.activity.getString(R.string.note_editor_link_insert)
        composeTestRule.onNodeWithText(insertText).assertIsNotEnabled()
    }

    @Test
    fun `insert-link display-text placeholder follows the in-app language override`() {
        composeTestRule.setContent {
            LocalizedContent(language = AppLanguage.Italian) {
                NoteEditorContent(
                    uiState = baseState(isEditing = true).copy(isLinkDialogVisible = true, linkUrl = ""),
                    onEvent = {},
                )
            }
        }

        // Regression test: the field's placeholder is resolved inside AlertDialog's content slot,
        // which Compose composes in a separate window that re-provides LocalContext from the real
        // Activity context — shadowing LocalizedContent's override unless the caller resolves the
        // string outside that boundary. Device/test locale here is English, so seeing the Italian
        // string confirms the override, not a device-locale fallback.
        composeTestRule.onNodeWithText("Testo da visualizzare").assertExists()
    }

    @Test
    fun `insert-link fields do not shrink once text is typed`() {
        val displayText = mutableStateOf("")
        val url = mutableStateOf("")
        val base = baseState(isEditing = true)
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = base.copy(
                    isLinkDialogVisible = true,
                    linkDisplayText = displayText.value,
                    linkUrl = url.value,
                ),
                onEvent = {},
            )
        }

        val emptyDisplayHeight = composeTestRule.onNodeWithTag(LINK_DISPLAY_TEXT_FIELD_TAG)
            .getUnclippedBoundsInRoot().height
        val emptyUrlHeight = composeTestRule.onNodeWithTag(LINK_URL_FIELD_TAG)
            .getUnclippedBoundsInRoot().height

        displayText.value = "Example"
        url.value = "https://example.com"
        composeTestRule.waitForIdle()

        val typedDisplayHeight = composeTestRule.onNodeWithTag(LINK_DISPLAY_TEXT_FIELD_TAG)
            .getUnclippedBoundsInRoot().height
        val typedUrlHeight = composeTestRule.onNodeWithTag(LINK_URL_FIELD_TAG)
            .getUnclippedBoundsInRoot().height

        assertThat(typedDisplayHeight.value).isWithin(0.5f).of(emptyDisplayHeight.value)
        assertThat(typedUrlHeight.value).isWithin(0.5f).of(emptyUrlHeight.value)
    }

    @Test
    fun `edit mode toolbar shows all 8 controls plus both dividers`() {
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState(isEditing = true), onEvent = {})
        }

        composeTestRule.onNodeWithTag(TOOLBAR_COLOR_DIVIDER_TAG, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(TOOLBAR_LINK_DIVIDER_TAG, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.note_editor_bold_action),
        ).assertExists()
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.note_editor_italic_action),
        ).assertExists()
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.note_editor_underline_action),
        ).assertExists()
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.note_editor_heading_action),
        ).assertExists()
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.note_editor_bullet_action),
        ).assertExists()
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.note_editor_numbered_list_action),
        ).assertExists()
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.note_editor_link_action),
        ).assertExists()
    }

    @Test
    fun `body placeholder shows only on the first, empty segment`() {
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState(isEditing = true).copy(
                    segments = listOf(
                        EditorSegment.Rich(id = "p1", state = newRichTextState()),
                        EditorSegment.Rich(id = "p2", state = newRichTextState()),
                    ),
                ),
                onEvent = {},
            )
        }

        val placeholder = composeTestRule.activity.getString(R.string.note_editor_body_placeholder)
        composeTestRule.onAllNodesWithText(placeholder).assertCountEquals(1)
    }

    @Test
    fun `body placeholder shows in read mode for a saved note with an empty body`() {
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState(isEditing = false).copy(
                    segments = listOf(EditorSegment.Rich(id = "p1", state = newRichTextState())),
                ),
                onEvent = {},
            )
        }

        val placeholder = composeTestRule.activity.getString(R.string.note_editor_body_placeholder)
        composeTestRule.onNodeWithText(placeholder).assertIsDisplayed()
    }

    @Test
    fun `heading popover shows H1, H2, and H3 when visible`() {
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState(isEditing = true).copy(isHeadingPopoverVisible = true), onEvent = {})
        }

        composeTestRule.onNodeWithText("H1").assertExists()
        composeTestRule.onNodeWithText("H2").assertExists()
        composeTestRule.onNodeWithText("H3").assertExists()
    }

    private val moveFolders = listOf(
        MoveFolderRowUi(id = ROOT_FOLDER_ID, name = "", depth = 0, isLocked = false, isCurrent = false),
        MoveFolderRowUi(id = "journal", name = "Journal", depth = 1, isLocked = true, isCurrent = false),
        MoveFolderRowUi(id = "personal", name = "Personal", depth = 1, isLocked = false, isCurrent = true),
    )

    @Test
    fun `move sheet shows the top-level row, folders, and the Current label`() {
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState().copy(isMoveSheetVisible = true, moveFolders = moveFolders),
                onEvent = {},
            )
        }

        val topLevel = composeTestRule.activity.getString(R.string.move_sheet_top_level)
        composeTestRule.onNodeWithText(topLevel).assertExists()
        composeTestRule.onNodeWithText("Journal").assertExists()
        composeTestRule.onNodeWithText("Personal").assertExists()
        val current = composeTestRule.activity.getString(R.string.move_sheet_current)
        composeTestRule.onNodeWithText(current).assertExists()
    }

    @Test
    fun `Move button is disabled until a folder is selected`() {
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState().copy(
                    isMoveSheetVisible = true,
                    moveFolders = moveFolders,
                    selectedMoveFolderId = null,
                ),
                onEvent = {},
            )
        }

        val moveText = composeTestRule.activity.getString(R.string.move_sheet_move)
        composeTestRule.onNodeWithText(moveText).assertIsNotEnabled()
    }

    @Test
    fun `tapping a selectable folder row raises MoveFolderSelected`() {
        var selectedId: String? = null
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState().copy(isMoveSheetVisible = true, moveFolders = moveFolders),
                onEvent = { if (it is NoteEditorEvent.MoveFolderSelected) selectedId = it.folderId },
            )
        }

        composeTestRule.onNodeWithText("Journal").performClick()

        assertThat(selectedId).isEqualTo("journal")
    }

    @Test
    fun `tapping Move raises MoveConfirmed`() {
        var confirmed = false
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState().copy(
                    isMoveSheetVisible = true,
                    moveFolders = moveFolders,
                    selectedMoveFolderId = "journal",
                ),
                onEvent = { if (it == NoteEditorEvent.MoveConfirmed) confirmed = true },
            )
        }

        val moveText = composeTestRule.activity.getString(R.string.move_sheet_move)
        composeTestRule.onNodeWithText(moveText).performClick()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun `delete confirmation dialog shows the quoted note title`() {
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState().copy(isDeleteDialogVisible = true), onEvent = {})
        }

        composeTestRule.onNodeWithText("“Groceries” will be kept in Trash for 30 days, then permanently deleted.")
            .assertExists()
    }

    @Test
    fun `delete confirmation dialog falls back to Untitled note when the title is blank`() {
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState().copy(title = "", isDeleteDialogVisible = true),
                onEvent = {},
            )
        }

        val untitled = composeTestRule.activity.getString(R.string.untitled_note)
        composeTestRule.onNodeWithText("“$untitled” will be kept in Trash for 30 days, then permanently deleted.")
            .assertExists()
    }

    @Test
    fun `tapping Move to Trash raises DeleteConfirmed`() {
        var confirmed = false
        composeTestRule.setContent {
            NoteEditorContent(
                uiState = baseState().copy(isDeleteDialogVisible = true),
                onEvent = { if (it == NoteEditorEvent.DeleteConfirmed) confirmed = true },
            )
        }

        val confirmText = composeTestRule.activity.getString(R.string.note_editor_delete_dialog_confirm)
        composeTestRule.onNodeWithText(confirmText).performClick()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun `back gesture in edit mode raises BackClicked`() {
        var receivedEvent: NoteEditorEvent? = null
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState(isEditing = true), onEvent = { receivedEvent = it })
        }

        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()

        assertThat(receivedEvent).isEqualTo(NoteEditorEvent.BackClicked)
    }

    @Test
    fun `back gesture in read mode is not intercepted by the editor`() {
        // Disabled in view mode so the system's predictive-back gesture drives Navigation
        // Compose's pop transition directly instead of a plain, non-seekable one (see
        // NoteEditorContent's BackHandler comment).
        var receivedEvent: NoteEditorEvent? = null
        composeTestRule.setContent {
            NoteEditorContent(uiState = baseState(isEditing = false), onEvent = { receivedEvent = it })
        }

        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()

        assertThat(receivedEvent).isNull()
    }

    // Regression coverage: Back must fully dismiss the keyboard before the note closes, rather
    // than letting the IME animate away together with the outgoing screen (see
    // NoteEditorContent's onBackRequested). Robolectric reports no real IME inset, so the wait
    // for it to settle back to 0 resolves immediately and BackClicked still fires.
    @Test
    fun `back gesture in edit mode hides the keyboard before raising BackClicked`() {
        val fakeKeyboard = FakeSoftwareKeyboardController()
        var receivedEvent: NoteEditorEvent? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides fakeKeyboard) {
                NoteEditorContent(uiState = baseState(isEditing = true), onEvent = { receivedEvent = it })
            }
        }

        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()

        assertThat(fakeKeyboard.hideCount).isAtLeast(1)
        assertThat(receivedEvent).isEqualTo(NoteEditorEvent.BackClicked)
    }

    @Test
    fun `tapping the top-bar back arrow hides the keyboard and raises BackClicked`() {
        val fakeKeyboard = FakeSoftwareKeyboardController()
        var receivedEvent: NoteEditorEvent? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides fakeKeyboard) {
                NoteEditorContent(uiState = baseState(isEditing = true), onEvent = { receivedEvent = it })
            }
        }

        val backDescription = composeTestRule.activity.getString(R.string.note_editor_back_action)
        composeTestRule.onNodeWithContentDescription(backDescription).performClick()
        composeTestRule.waitForIdle()

        assertThat(fakeKeyboard.hideCount).isAtLeast(1)
        assertThat(receivedEvent).isEqualTo(NoteEditorEvent.BackClicked)
    }

    // Regression coverage for the back-gesture-dismisses-keyboard bug: dismissing the IME with
    // Back leaves the field focused (no onFocusChanged transition), so tapping it again must
    // still re-show the keyboard explicitly rather than relying on a focus change that won't fire.
    @Test
    fun `tapping the title field in edit mode re-shows the keyboard`() {
        val fakeKeyboard = FakeSoftwareKeyboardController()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides fakeKeyboard) {
                NoteEditorContent(uiState = baseState(isEditing = true), onEvent = {})
            }
        }

        composeTestRule.onNodeWithTag(NOTE_TITLE_FIELD_TAG).performClick()

        assertThat(fakeKeyboard.showCount).isAtLeast(1)
    }

    @Test
    fun `tapping a body editor in edit mode re-shows the keyboard`() {
        val fakeKeyboard = FakeSoftwareKeyboardController()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides fakeKeyboard) {
                NoteEditorContent(uiState = baseState(isEditing = true), onEvent = {})
            }
        }

        composeTestRule.onNodeWithText("Milk").performClick()

        assertThat(fakeKeyboard.showCount).isAtLeast(1)
    }

    // A blank note's single empty segment is already focused, so most of the "blank" area a user
    // taps (e.g. below the cursor) is the outer empty-space handler, not the segment's own tiny hit
    // box — that dispatches EditModeRequested, which re-sets pendingFocus on the same, already
    // focused segment. requestFocus() no-ops in that case and fires no onFocusChanged, so the
    // pendingFocus effect must show the keyboard unconditionally rather than relying on a focus
    // transition that won't happen.
    @Test
    fun `pending focus on an already-focused segment still shows the keyboard`() {
        val fakeKeyboard = FakeSoftwareKeyboardController()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides fakeKeyboard) {
                NoteEditorContent(
                    uiState = baseState(isEditing = true).copy(pendingFocus = EditorFocusTarget.Segment("p1")),
                    onEvent = {},
                )
            }
        }

        composeTestRule.waitForIdle()

        assertThat(fakeKeyboard.showCount).isAtLeast(1)
    }

    private class FakeSoftwareKeyboardController : SoftwareKeyboardController {
        var showCount = 0
            private set
        var hideCount = 0
            private set

        override fun show() {
            showCount++
        }

        override fun hide() {
            hideCount++
        }
    }

    private class FakeUriHandler(private val onOpen: (String) -> Unit) : UriHandler {
        override fun openUri(uri: String) = onOpen(uri)
    }
}
