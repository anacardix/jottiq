package com.anacardix.jottiq.ui.noteeditor

import com.anacardix.jottiq.MainDispatcherRule
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
 * Apple-Notes-style structural editor behaviors — bullet toggling and caret-focus targeting.
 * Split out of [NoteEditorViewModelTest] (see there for loading, formatting toolbar, links,
 * favorite/lock, delete, and move tests).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NoteEditorStructuralEditsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notesRepository = FakeNotesRepository()
    private val foldersRepository = FakeFoldersRepository()

    private fun viewModel(noteId: String = "note-1") =
        testViewModel(notesRepository, foldersRepository, noteId = noteId)

    private fun seedNote(note: Note) {
        notesRepository.setNotes(listOf(note))
    }

    // Regression test for the full "blank note -> new bulleted list -> exit list -> reopen" flow:
    // toggling bullet on, typing text, pressing Enter (grows the list), then pressing Enter again
    // on the new empty item (exits the list into a blank paragraph, per
    // `config.exitListOnEmptyItem` in `NoteDocumentBridge.kt`) must persist a plain, non-bulleted
    // trailing paragraph -- and a *fresh* ViewModel loading that same note back must keep it plain,
    // not resurrect it as a second bulleted (empty) line.
    @Test
    fun `exiting a bulleted list on an empty item stays unbulleted after the note is reopened`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))
        viewModel.onEvent(NoteEditorEvent.BulletClicked)

        val state = (viewModel.uiState.value.segments.single() as EditorSegment.Rich).state
        state.addTextAfterSelection("aaa")
        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segmentId))
        state.addTextAfterSelection("\n")
        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segmentId))
        state.addTextAfterSelection("\n")
        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segmentId))

        viewModel.onEvent(NoteEditorEvent.BackClicked)
        advanceUntilIdle()

        val persisted = notesRepository.allNotes.single().document.blocks.map { it as NoteBlock.Paragraph }
        assertThat(persisted).hasSize(2)
        assertThat(persisted[0].text).isEqualTo("aaa")
        assertThat(persisted[0].bulleted).isTrue()
        assertThat(persisted[1].text).isEmpty()
        assertThat(persisted[1].bulleted).isFalse()

        val reloaded = viewModel(noteId = "note-1")
        reloaded.onEvent(NoteEditorEvent.ScreenShown)
        val reloadedSegments = reloaded.uiState.value.segments
        assertThat(reloadedSegments).hasSize(1)
        val reloadedState = (reloadedSegments.single() as EditorSegment.Rich).state
        val reloadedBlocks = reloadedState.paragraphBlocks()
        assertThat(reloadedBlocks).hasSize(2)
        assertThat(reloadedBlocks[0].bulleted).isTrue()
        assertThat(reloadedBlocks[1].bulleted).isFalse()
        assertThat(reloadedState.toHtml()).doesNotContain("<li></li>")
    }

    // Numbered-list counterpart of the bulleted-list regression above — same "toggle on -> type ->
    // Enter (grows the list) -> Enter on the empty item (exits into a blank paragraph)" flow, using
    // toggleOrderedList()/removeOrderedList() instead. Guards the same padding-strip fix in
    // `EditorBlockHelpers.kt`'s `richSegment` for numbered lines.
    @Test
    fun `exiting a numbered list on an empty item stays unnumbered after the note is reopened`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))
        viewModel.onEvent(NoteEditorEvent.NumberedListClicked)

        val state = (viewModel.uiState.value.segments.single() as EditorSegment.Rich).state
        state.addTextAfterSelection("aaa")
        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segmentId))
        state.addTextAfterSelection("\n")
        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segmentId))
        state.addTextAfterSelection("\n")
        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segmentId))

        viewModel.onEvent(NoteEditorEvent.BackClicked)
        advanceUntilIdle()

        val persisted = notesRepository.allNotes.single().document.blocks.map { it as NoteBlock.Paragraph }
        assertThat(persisted).hasSize(2)
        assertThat(persisted[0].text).isEqualTo("aaa")
        assertThat(persisted[0].numbered).isTrue()
        assertThat(persisted[1].text).isEmpty()
        assertThat(persisted[1].numbered).isFalse()

        val reloaded = viewModel(noteId = "note-1")
        reloaded.onEvent(NoteEditorEvent.ScreenShown)
        val reloadedSegments = reloaded.uiState.value.segments
        assertThat(reloadedSegments).hasSize(1)
        val reloadedState = (reloadedSegments.single() as EditorSegment.Rich).state
        val reloadedBlocks = reloadedState.paragraphBlocks()
        assertThat(reloadedBlocks).hasSize(2)
        assertThat(reloadedBlocks[0].numbered).isTrue()
        assertThat(reloadedBlocks[1].numbered).isFalse()
        assertThat(reloadedState.toHtml()).doesNotContain("<li></li>")
    }

    @Test
    fun `EditModeRequested on a segment starts editing with the caret pending there`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id

        viewModel.onEvent(NoteEditorEvent.EditModeRequested(segmentId))

        val state = viewModel.uiState.value
        assertThat(state.isEditing).isTrue()
        assertThat(state.pendingFocus).isEqualTo(EditorFocusTarget.Segment(segmentId))
    }

    @Test
    fun `EditModeRequested without a segment targets the end of the note`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id

        viewModel.onEvent(NoteEditorEvent.EditModeRequested())

        assertThat(viewModel.uiState.value.pendingFocus).isEqualTo(EditorFocusTarget.Segment(segmentId))
    }

    @Test
    fun `TitleNextPressed moves the pending caret to the first segment`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Milk")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id

        viewModel.onEvent(NoteEditorEvent.TitleNextPressed)

        assertThat(viewModel.uiState.value.pendingFocus).isEqualTo(EditorFocusTarget.Segment(segmentId))
    }

    @Test
    fun `FocusRequestConsumed clears the pending focus`() = runTest {
        seedNote(note(title = "", document = NoteDocument()))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        assertThat(viewModel.uiState.value.pendingFocus).isNotNull()

        viewModel.onEvent(NoteEditorEvent.FocusRequestConsumed)

        assertThat(viewModel.uiState.value.pendingFocus).isNull()
    }
}
