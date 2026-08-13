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
 * Autosave scheduling and doPersist()'s cached partial reserialize (see NoteEditorViewModel's
 * dirtySegmentIds / NoteDocumentBridge's toNoteDocumentCached): a segment's HTML round-trip is
 * skipped on save unless that segment's id was marked dirty since the last save. Split out of
 * [NoteEditorViewModelTest] (which covers loading, formatting toolbar, links, favorite/lock,
 * delete, and move) to keep that class under detekt's size threshold.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NoteEditorAutosaveCacheTest {

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
    fun `SegmentContentChanged autosaves the live segment content after the debounce pause`() = runTest {
        seedNote(note(document = NoteDocument()))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segment = viewModel.uiState.value.segments.single() as EditorSegment.Rich
        segment.state.addTextAfterSelection("Farmers market")

        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segment.id))
        advanceUntilIdle()

        val persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.text).isEqualTo("Farmers market")
        assertThat(persisted.bulleted).isFalse()
    }

    @Test
    fun `a save triggered by an unrelated field reuses the segment's cached content`() = runTest {
        seedNote(
            note(
                document = NoteDocument(
                    blocks = listOf(
                        NoteBlock.Paragraph(id = "p1", text = "First"),
                        NoteBlock.Paragraph(id = "p2", text = "Milk"),
                    ),
                ),
                isFavorite = false,
            ),
        )
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segment = viewModel.uiState.value.segments.single() as EditorSegment.Rich
        segment.state.addTextAtIndex(segment.state.annotatedString.text.length, " edited")
        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segment.id))
        advanceUntilIdle()

        // Favoriting persists immediately but touches no segment, so the note's blocks should come
        // straight from the cache populated by the save above.
        viewModel.onEvent(NoteEditorEvent.FavoriteClicked)

        val blocks = notesRepository.allNotes.single().document.blocks.map { it as NoteBlock.Paragraph }
        assertThat(blocks.map { it.text }).containsExactly("First", "Milk edited").inOrder()
    }
}
