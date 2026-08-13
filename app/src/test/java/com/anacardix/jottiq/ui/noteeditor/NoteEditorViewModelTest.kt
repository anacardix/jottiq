package com.anacardix.jottiq.ui.noteeditor

import app.cash.turbine.test
import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.DataError
import com.anacardix.jottiq.domain.FormatSpan
import com.anacardix.jottiq.domain.FormatStyle
import com.anacardix.jottiq.domain.Note
import com.anacardix.jottiq.domain.NoteBlock
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.domain.NoteTextColor
import com.anacardix.jottiq.fakes.FakeFoldersRepository
import com.anacardix.jottiq.fakes.FakeNotesRepository
import com.anacardix.jottiq.fakes.folder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Loading, formatting toolbar, links, favorite/lock, delete, and move behaviors. See
 * [NoteEditorStructuralEditsTest] for bullet toggling and caret-focus targeting,
 * [NoteEditorHeadingTest] for heading toggle behavior, and [NoteEditorAutosaveCacheTest] for
 * autosave scheduling and the cached partial reserialize.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NoteEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notesRepository = FakeNotesRepository()
    private val foldersRepository = FakeFoldersRepository()

    private fun viewModel(noteId: String = "note-1") =
        testViewModel(notesRepository, foldersRepository, noteId = noteId)

    private fun seedNote(note: Note) {
        notesRepository.setNotes(listOf(note))
    }

    @Test
    fun `initial state is loading and carries the requested note id`() = runTest {
        val state = viewModel(noteId = "note-1").uiState.value

        assertThat(state.isLoading).isTrue()
        assertThat(state.noteId).isEqualTo("note-1")
    }

    @Test
    fun `ScreenShown loads title, flags, and segments`() = runTest {
        seedNote(
            note(
                title = "Groceries",
                isFavorite = true,
                document = NoteDocument(
                    blocks = listOf(
                        NoteBlock.Paragraph(id = "p1", text = "Farmers market"),
                        NoteBlock.Paragraph(id = "p2", text = "Milk"),
                    ),
                ),
            ),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()

            viewModel.onEvent(NoteEditorEvent.ScreenShown)

            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.title).isEqualTo("Groceries")
            assertThat(loaded.isFavorite).isTrue()
            assertThat(loaded.segments).hasSize(1)
            assertThat((loaded.segments.single() as EditorSegment.Rich).state.toText())
                .isEqualTo("Farmers market\nMilk")
        }
    }

    @Test
    fun `TitleChanged updates the ui state immediately`() = runTest {
        seedNote(note())
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.TitleChanged("New title"))

        assertThat(viewModel.uiState.value.title).isEqualTo("New title")
    }

    @Test
    fun `title edits autosave after the debounce pause, not per keystroke`() = runTest {
        seedNote(note())
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.TitleChanged("N"))
        viewModel.onEvent(NoteEditorEvent.TitleChanged("Ne"))
        viewModel.onEvent(NoteEditorEvent.TitleChanged("New title"))
        assertThat(notesRepository.allNotes.single().title).isEqualTo("Groceries")

        advanceUntilIdle()

        assertThat(notesRepository.allNotes.single().title).isEqualTo("New title")
        assertThat(notesRepository.updateNoteCallCount).isEqualTo(1)
    }

    @Test
    fun `FavoriteClicked toggles and persists immediately`() = runTest {
        seedNote(note(isFavorite = false))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.FavoriteClicked)

        assertThat(viewModel.uiState.value.isFavorite).isTrue()
        assertThat(notesRepository.allNotes.single().isFavorite).isTrue()
    }

    @Test
    fun `FavoriteClicked does not touch updatedAt (favoriting is metadata, not a content edit)`() = runTest {
        seedNote(note(isFavorite = false).copy(updatedAt = 42L))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.FavoriteClicked)

        assertThat(notesRepository.allNotes.single().updatedAt).isEqualTo(42L)
        assertThat(notesRepository.updateNoteCallCount).isEqualTo(0)
    }

    @Test
    fun `a failed favorite toggle reverts the ui state and surfaces an error`() = runTest {
        seedNote(note(isFavorite = false))
        notesRepository.setFavoriteFailure = DataError.Unknown
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.FavoriteClicked)

        assertThat(viewModel.uiState.value.isFavorite).isFalse()
        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.note_editor_error_save)
    }

    @Test
    fun `LockClicked toggles and persists immediately`() = runTest {
        seedNote(note(isLocked = false))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.LockClicked)

        assertThat(viewModel.uiState.value.isLocked).isTrue()
        assertThat(notesRepository.allNotes.single().isLocked).isTrue()
    }

    @Test
    fun `LockClicked does not touch updatedAt (locking is metadata, not a content edit)`() = runTest {
        seedNote(note(isLocked = false).copy(updatedAt = 42L))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.LockClicked)

        assertThat(notesRepository.allNotes.single().updatedAt).isEqualTo(42L)
        assertThat(notesRepository.updateNoteCallCount).isEqualTo(0)
    }

    @Test
    fun `a failed lock toggle reverts the ui state and surfaces an error`() = runTest {
        seedNote(note(isLocked = false))
        notesRepository.setLockedFailure = DataError.Unknown
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.LockClicked)

        assertThat(viewModel.uiState.value.isLocked).isFalse()
        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.note_editor_error_save)
    }

    @Test
    fun `bold toggles over the current selection and persists the span`() = runTest {
        seedNote(
            note(
                document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Hello world"))),
            ),
        )
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))
        selectRange(viewModel, segmentId, 0, 5)

        viewModel.onEvent(NoteEditorEvent.BoldClicked)

        val persistedNote = notesRepository.allNotes.single()
        val persisted = persistedNote.document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.spans).contains(FormatSpan(0, 5, FormatStyle.Bold))
    }

    @Test
    fun `underline skips emoji within the selection`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Hi😀!")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))
        selectRange(viewModel, segmentId, 0, 5)

        viewModel.onEvent(NoteEditorEvent.UnderlineClicked)

        val persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.text).isEqualTo("Hi😀!")
        assertThat(persisted.spans).containsExactly(
            FormatSpan(0, 2, FormatStyle.Underline),
            FormatSpan(4, 5, FormatStyle.Underline),
        )
    }

    @Test
    fun `color selection skips emoji within the selection`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Hi😀!")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))
        selectRange(viewModel, segmentId, 0, 5)

        viewModel.onEvent(NoteEditorEvent.ColorSelected(NoteTextColor.Gold))

        val persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.spans).containsExactly(
            FormatSpan(0, 2, FormatStyle.TextColor(NoteTextColor.Gold)),
            FormatSpan(4, 5, FormatStyle.TextColor(NoteTextColor.Gold)),
        )
    }

    @Test
    fun `color selection applies a color span over the selection`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Hello world")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))
        selectRange(viewModel, segmentId, 0, 5)

        viewModel.onEvent(NoteEditorEvent.ColorSelected(NoteTextColor.Gold))

        assertThat(viewModel.uiState.value.isColorPopoverVisible).isFalse()
        val persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.spans).contains(
            FormatSpan(0, 5, FormatStyle.TextColor(NoteTextColor.Gold)),
        )
    }

    @Test
    fun `BulletClicked toggles bulleted on the focused segment and persists`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))

        viewModel.onEvent(NoteEditorEvent.BulletClicked)

        val persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.bulleted).isTrue()
    }

    @Test
    fun `BulletClicked does nothing when no segment is focused`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.BulletClicked)

        val blocks = notesRepository.allNotes.singleOrNull()?.document?.blocks
        val persisted = blocks?.singleOrNull() as? NoteBlock.Paragraph
        assertThat(persisted?.bulleted ?: false).isFalse()
    }

    @Test
    fun `NumberedListClicked toggles numbered on the focused segment and persists`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))

        viewModel.onEvent(NoteEditorEvent.NumberedListClicked)

        val persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.numbered).isTrue()
    }

    @Test
    fun `NumberedListClicked does nothing when no segment is focused`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.NumberedListClicked)

        val blocks = notesRepository.allNotes.singleOrNull()?.document?.blocks
        val persisted = blocks?.singleOrNull() as? NoteBlock.Paragraph
        assertThat(persisted?.numbered ?: false).isFalse()
    }

    @Test
    fun `NumberedListClicked on a bulleted line replaces the bullet with numbering`() = runTest {
        val block = NoteBlock.Paragraph(id = "p1", text = "Milk", bulleted = true)
        seedNote(note(document = NoteDocument(blocks = listOf(block))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))

        viewModel.onEvent(NoteEditorEvent.NumberedListClicked)

        val persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.numbered).isTrue()
        assertThat(persisted.bulleted).isFalse()
    }

    @Test
    fun `a brand-new note seeds one empty rich segment`() = runTest {
        seedNote(note(document = NoteDocument()))
        val viewModel = viewModel()

        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        val segment = viewModel.uiState.value.segments.single() as EditorSegment.Rich
        assertThat(segment.state.annotatedString.text).isEmpty()
    }

    @Test
    fun `a brand-new note opens in edit mode with the caret pending on the title`() = runTest {
        seedNote(note(title = "", document = NoteDocument()))
        val viewModel = viewModel()

        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        val state = viewModel.uiState.value
        assertThat(state.isEditing).isTrue()
        assertThat(state.wasEdited).isFalse()
        assertThat(state.pendingFocus).isEqualTo(EditorFocusTarget.Title)
    }

    @Test
    fun `a note updated after creation shows the Edited label`() = runTest {
        seedNote(
            note(title = "Groceries").copy(createdAt = 0L, updatedAt = 60_000L),
        )
        val viewModel = viewModel()

        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        assertThat(viewModel.uiState.value.wasEdited).isTrue()
    }

    @Test
    fun `an existing note with a title opens in read mode`() = runTest {
        seedNote(note(title = "Groceries", document = NoteDocument()))
        val viewModel = viewModel()

        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        assertThat(viewModel.uiState.value.isEditing).isFalse()
    }

    @Test
    fun `LinkClicked prefills the display text from the current selection`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Click here")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))
        selectRange(viewModel, segmentId, 6, 10)

        viewModel.onEvent(NoteEditorEvent.LinkClicked)

        assertThat(viewModel.uiState.value.isLinkDialogVisible).isTrue()
        assertThat(viewModel.uiState.value.linkDisplayText).isEqualTo("here")
    }

    @Test
    fun `LinkInsertConfirmed inserts linked text at the cursor and closes the dialog`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))

        viewModel.onEvent(NoteEditorEvent.LinkDisplayTextChanged("Example"))
        viewModel.onEvent(NoteEditorEvent.LinkUrlChanged("https://example.com"))
        viewModel.onEvent(NoteEditorEvent.LinkInsertConfirmed)

        assertThat(viewModel.uiState.value.isLinkDialogVisible).isFalse()
        val persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.text).isEqualTo("Example")
        assertThat(persisted.spans).contains(
            FormatSpan(0, 7, FormatStyle.Link("https://example.com")),
        )
    }

    @Test
    fun `LinkInsertConfirmed normalizes a scheme-less url to https before storing it`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))

        viewModel.onEvent(NoteEditorEvent.LinkDisplayTextChanged("Example"))
        viewModel.onEvent(NoteEditorEvent.LinkUrlChanged("example.com"))
        viewModel.onEvent(NoteEditorEvent.LinkInsertConfirmed)

        val persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.spans).contains(
            FormatSpan(0, 7, FormatStyle.Link("https://example.com")),
        )
    }

    @Test
    fun `LinkInsertConfirmed falls back to the last segment when nothing is focused`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.LinkUrlChanged("https://example.com"))
        viewModel.onEvent(NoteEditorEvent.LinkInsertConfirmed)

        val persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.text).isEqualTo("https://example.com")
    }

    @Test
    fun `BackClicked emits the Back navigation event`() = runTest {
        val viewModel = viewModel()

        viewModel.navigationEvents.test {
            viewModel.onEvent(NoteEditorEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(NoteEditorNavigationEvent.Back)
        }
    }

    @Test
    fun `BackClicked on a fully empty note moves it to trash`() = runTest {
        seedNote(note(id = "note-1", title = "", document = NoteDocument()))
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(NoteEditorEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(NoteEditorNavigationEvent.Back)
        }
        assertThat(notesRepository.allNotes.single().deletedAt).isNotNull()
    }

    @Test
    fun `a pending autosave cannot resurrect a note discarded on back`() = runTest {
        seedNote(note(id = "note-1", title = "", document = NoteDocument()))
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        // A scheduled (but not yet fired) autosave, e.g. text typed and immediately deleted.
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segmentId))

        viewModel.onEvent(NoteEditorEvent.BackClicked)
        advanceUntilIdle()

        assertThat(notesRepository.allNotes.single().deletedAt).isNotNull()
    }

    @Test
    fun `BackClicked flushes a pending autosave before navigating`() = runTest {
        seedNote(note(id = "note-1", title = "Groceries"))
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.TitleChanged("Groceries and more"))

        viewModel.onEvent(NoteEditorEvent.BackClicked)

        assertThat(notesRepository.allNotes.single().title).isEqualTo("Groceries and more")
    }

    @Test
    fun `BackClicked without edits never writes to the repository`() = runTest {
        seedNote(note(id = "note-1", title = "Groceries"))
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.BackClicked)
        advanceUntilIdle()

        assertThat(notesRepository.updateNoteCallCount).isEqualTo(0)
    }

    @Test
    fun `BackClicked keeps a note with a title out of trash`() = runTest {
        seedNote(note(id = "note-1", title = "Groceries", document = NoteDocument()))
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.BackClicked)

        assertThat(notesRepository.allNotes.single().deletedAt).isNull()
    }

    @Test
    fun `BackClicked before the note finishes loading does not trash it`() = runTest {
        seedNote(note(id = "note-1", title = "Groceries"))
        val viewModel = viewModel(noteId = "note-1")

        viewModel.navigationEvents.test {
            viewModel.onEvent(NoteEditorEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(NoteEditorNavigationEvent.Back)
        }
        assertThat(notesRepository.allNotes.single().deletedAt).isNull()
    }

    @Test
    fun `BackClicked keeps a note with body text out of trash`() = runTest {
        seedNote(
            note(
                id = "note-1",
                title = "",
                document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk"))),
            ),
        )
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.BackClicked)

        assertThat(notesRepository.allNotes.single().deletedAt).isNull()
    }

    @Test
    fun `BackClicked while editing a note with content saves and leaves directly`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.EditModeRequested(segmentId))

        viewModel.navigationEvents.test {
            viewModel.onEvent(NoteEditorEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(NoteEditorNavigationEvent.Back)
        }
    }

    @Test
    fun `BackClicked while editing flushes pending edits before leaving`() = runTest {
        seedNote(note(title = "Groceries"))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.EditModeRequested(segmentId))
        viewModel.onEvent(NoteEditorEvent.TitleChanged("Groceries and more"))

        viewModel.navigationEvents.test {
            viewModel.onEvent(NoteEditorEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(NoteEditorNavigationEvent.Back)
        }
        assertThat(notesRepository.allNotes.single().title).isEqualTo("Groceries and more")
    }

    @Test
    fun `BackClicked while editing an empty new note still discards and leaves`() = runTest {
        seedNote(note(title = "", document = NoteDocument()))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        assertThat(viewModel.uiState.value.isEditing).isTrue()

        viewModel.navigationEvents.test {
            viewModel.onEvent(NoteEditorEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(NoteEditorNavigationEvent.Back)
        }
        assertThat(notesRepository.allNotes.single().deletedAt).isNotNull()
    }

    @Test
    fun `DeleteClicked opens the confirmation dialog without deleting anything yet`() = runTest {
        seedNote(note())
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.DeleteClicked)

        assertThat(viewModel.uiState.value.isDeleteDialogVisible).isTrue()
        assertThat(notesRepository.allNotes.single().deletedAt).isNull()
    }

    @Test
    fun `DeleteDialogDismissed closes the dialog without deleting`() = runTest {
        seedNote(note())
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.DeleteClicked)

        viewModel.onEvent(NoteEditorEvent.DeleteDialogDismissed)

        assertThat(viewModel.uiState.value.isDeleteDialogVisible).isFalse()
        assertThat(notesRepository.allNotes.single().deletedAt).isNull()
    }

    @Test
    fun `DeleteConfirmed moves the note to trash and navigates back`() = runTest {
        seedNote(note(id = "note-1"))
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.DeleteClicked)

        viewModel.navigationEvents.test {
            viewModel.onEvent(NoteEditorEvent.DeleteConfirmed)

            assertThat(awaitItem()).isEqualTo(NoteEditorNavigationEvent.Back)
        }
        assertThat(viewModel.uiState.value.isDeleteDialogVisible).isFalse()
        assertThat(notesRepository.allNotes.single().deletedAt).isNotNull()
    }

    @Test
    fun `a pending autosave cannot resurrect a note deleted from the dialog`() = runTest {
        seedNote(note(id = "note-1"))
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.TitleChanged("Edited just before deleting"))

        viewModel.onEvent(NoteEditorEvent.DeleteConfirmed)
        advanceUntilIdle()

        assertThat(notesRepository.allNotes.single().deletedAt).isNotNull()
    }

    @Test
    fun `a failed delete surfaces a user message and keeps the note active`() = runTest {
        seedNote(note(id = "note-1"))
        notesRepository.moveToTrashFailure = DataError.Unknown
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.DeleteClicked)

        viewModel.onEvent(NoteEditorEvent.DeleteConfirmed)

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.note_editor_error_save)
        assertThat(notesRepository.allNotes.single().deletedAt).isNull()
    }

    @Test
    fun `a failed save surfaces a user message`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))

        viewModel.uiState.test {
            awaitItem()
            notesRepository.updateNoteFailure = DataError.Unknown
            viewModel.onEvent(NoteEditorEvent.BoldClicked)

            val withMessage = awaitItem()
            assertThat(withMessage.userMessage?.messageResId).isEqualTo(R.string.note_editor_error_save)
        }
    }

    @Test
    fun `MoveClicked loads the folder tree, marking the note's current folder`() = runTest {
        seedNote(note(folderId = "personal"))
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "travel", parentId = "personal", name = "Travel"),
                folder(id = "work", name = "Work"),
            ),
        )
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.onEvent(NoteEditorEvent.MoveClicked)

        val state = viewModel.uiState.value
        assertThat(state.isMoveSheetVisible).isTrue()
        assertThat(state.moveFolders.map { it.id }).containsExactly(ROOT_FOLDER_ID, "personal", "travel", "work")
        assertThat(state.moveFolders.first { it.id == "personal" }.isCurrent).isTrue()
        assertThat(state.moveFolders.first { it.id == ROOT_FOLDER_ID }.isCurrent).isFalse()
    }

    @Test
    fun `the current folder row cannot be selected`() = runTest {
        seedNote(note(folderId = "personal"))
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.MoveClicked)

        viewModel.onEvent(NoteEditorEvent.MoveFolderSelected("personal"))

        assertThat(viewModel.uiState.value.selectedMoveFolderId).isNull()
    }

    @Test
    fun `selecting a folder and confirming moves the note and shows a message`() = runTest {
        seedNote(note(folderId = null))
        foldersRepository.setFolders(listOf(folder(id = "work", name = "Work")))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.MoveClicked)

        viewModel.onEvent(NoteEditorEvent.MoveFolderSelected("work"))
        viewModel.onEvent(NoteEditorEvent.MoveConfirmed)

        assertThat(notesRepository.allNotes.single().folderId).isEqualTo("work")
        val state = viewModel.uiState.value
        assertThat(state.isMoveSheetVisible).isFalse()
        assertThat(state.userMessage?.messageResId).isEqualTo(R.string.note_editor_moved_to_folder)
        assertThat(state.userMessage?.formatArgs).containsExactly("Work")
    }

    @Test
    fun `MoveConfirmed does not touch updatedAt (moving is organizational, not a content edit)`() = runTest {
        seedNote(note(folderId = null).copy(updatedAt = 42L))
        foldersRepository.setFolders(listOf(folder(id = "work", name = "Work")))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.MoveClicked)
        viewModel.onEvent(NoteEditorEvent.MoveFolderSelected("work"))

        viewModel.onEvent(NoteEditorEvent.MoveConfirmed)

        assertThat(notesRepository.allNotes.single().updatedAt).isEqualTo(42L)
        assertThat(notesRepository.updateNoteCallCount).isEqualTo(0)
    }

    @Test
    fun `selecting top level and confirming clears the note's folder`() = runTest {
        seedNote(note(folderId = "personal"))
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.MoveClicked)

        viewModel.onEvent(NoteEditorEvent.MoveFolderSelected(ROOT_FOLDER_ID))
        viewModel.onEvent(NoteEditorEvent.MoveConfirmed)

        assertThat(notesRepository.allNotes.single().folderId).isNull()
        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.note_editor_moved_to_root)
    }

    @Test
    fun `MoveSheetDismissed hides the sheet without moving the note`() = runTest {
        seedNote(note(folderId = null))
        foldersRepository.setFolders(listOf(folder(id = "work", name = "Work")))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.MoveClicked)
        viewModel.onEvent(NoteEditorEvent.MoveFolderSelected("work"))

        viewModel.onEvent(NoteEditorEvent.MoveSheetDismissed)

        assertThat(viewModel.uiState.value.isMoveSheetVisible).isFalse()
        assertThat(notesRepository.allNotes.single().folderId).isNull()
    }
}
