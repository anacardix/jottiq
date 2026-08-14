package com.anacardix.jottiq.ui.noteeditor

import androidx.compose.ui.text.TextRange
import app.cash.turbine.test
import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.DataError
import com.anacardix.jottiq.domain.Note
import com.anacardix.jottiq.domain.NoteBlock
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.fakes.FakeFoldersRepository
import com.anacardix.jottiq.fakes.FakeNotesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Back navigation and explicit delete: discarding a never-touched new note outright vs. trashing
 * an existing note. See [NoteEditorViewModelTest] for the rest of the editor's behaviors.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NoteEditorDiscardTest {

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
    fun `BackClicked emits the Back navigation event`() = runTest {
        val viewModel = viewModel()

        viewModel.navigationEvents.test {
            viewModel.onEvent(NoteEditorEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(NoteEditorNavigationEvent.Back)
        }
    }

    @Test
    fun `BackClicked on a never-edited empty note hard-deletes it instead of trashing it`() = runTest {
        seedNote(note(id = "note-1", title = "", document = NoteDocument()))
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(NoteEditorEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(NoteEditorNavigationEvent.Back)
        }
        assertThat(notesRepository.allNotes).isEmpty()
        assertThat(notesRepository.discardBlankNoteCallCount).isEqualTo(1)
    }

    @Test
    fun `BackClicked on an existing note cleared back to empty still moves it to trash`() = runTest {
        // Had real content before this session (createdAt != updatedAt, non-empty block), unlike a
        // just-created note — clearing it out must still go through the soft-delete trash path.
        seedNote(
            note(
                id = "note-1",
                title = "",
                document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk"))),
            ).copy(createdAt = 0L, updatedAt = 60_000L),
        )
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segment = viewModel.uiState.value.segments.single()
        segment.state.selection = TextRange(0, segment.state.annotatedString.text.length)
        segment.state.replaceSelectedText("")

        viewModel.onEvent(NoteEditorEvent.BackClicked)

        assertThat(notesRepository.allNotes.single().deletedAt).isNotNull()
        assertThat(notesRepository.discardBlankNoteCallCount).isEqualTo(0)
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

        assertThat(notesRepository.allNotes).isEmpty()
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
        assertThat(notesRepository.allNotes).isEmpty()
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
    fun `DeleteConfirmed on a never-edited empty note hard-deletes it instead of trashing it`() = runTest {
        seedNote(note(id = "note-1", title = "", document = NoteDocument()))
        val viewModel = viewModel(noteId = "note-1")
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        viewModel.onEvent(NoteEditorEvent.DeleteClicked)

        viewModel.navigationEvents.test {
            viewModel.onEvent(NoteEditorEvent.DeleteConfirmed)

            assertThat(awaitItem()).isEqualTo(NoteEditorNavigationEvent.Back)
        }
        assertThat(notesRepository.allNotes).isEmpty()
        assertThat(notesRepository.discardBlankNoteCallCount).isEqualTo(1)
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
}
