package com.anacardix.jottiq.ui.noteeditor

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.domain.HeadingLevel
import com.anacardix.jottiq.domain.Note
import com.anacardix.jottiq.domain.NoteBlock
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.fakes.FakeFoldersRepository
import com.anacardix.jottiq.fakes.FakeNotesRepository
import com.google.common.truth.Truth.assertThat
import com.mohamedrejeb.richeditor.model.HeadingStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Heading toggle behavior. Split out of [NoteEditorViewModelTest] (see there for loading,
 * formatting toolbar, links, favorite/lock, delete, and move tests).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NoteEditorHeadingTest {

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
    fun `heading toggles on the focused rich segment and persists`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Title")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))

        viewModel.onEvent(NoteEditorEvent.HeadingSelected(HeadingLevel.H1))
        var persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.heading).isEqualTo(HeadingLevel.H1)

        viewModel.onEvent(NoteEditorEvent.HeadingSelected(HeadingLevel.H1))
        persisted = notesRepository.allNotes.single().document.blocks.single() as NoteBlock.Paragraph
        assertThat(persisted.heading).isNull()
    }

    // Pins that toggling a heading updates the live (pre-persist, pre-reload) segment state
    // immediately via the library's own native heading mechanism — not just after the next
    // persist/reload.
    @Test
    fun `heading toggle sets the heading level immediately`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Title")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))

        viewModel.onEvent(NoteEditorEvent.HeadingSelected(HeadingLevel.H1))

        val segment = viewModel.uiState.value.segments.single() as EditorSegment.Rich
        assertThat(segment.state.currentHeadingStyle).isEqualTo(HeadingStyle.H1)
    }

    // Regression test for the reported bug: a previous implementation re-applied an absolute-size
    // span over a caret-offset range recomputed per heading paragraph — in a multi-paragraph rich
    // segment that range bookkeeping could drift and land on the wrong paragraph, so toggling a
    // heading on one line visibly resized a *different*, unrelated line (setting H1 on row 3 shrank
    // an H2 on row 2). Heading level is now toggled purely via the library's own per-paragraph
    // HeadingStyle at the caret — no per-paragraph span juggling — so toggling one paragraph's
    // heading must never change a sibling's.
    @Test
    fun `applying a heading to one paragraph does not change a sibling paragraph's heading`() = runTest {
        seedNote(
            note(
                document = NoteDocument(
                    blocks = listOf(
                        NoteBlock.Paragraph(id = "p1", text = "Row1"),
                        NoteBlock.Paragraph(id = "p2", text = "Row2", heading = HeadingLevel.H2),
                        NoteBlock.Paragraph(id = "p3", text = "Row3"),
                    ),
                ),
            ),
        )
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))
        val segment = viewModel.uiState.value.segments.single() as EditorSegment.Rich
        val row3Start = segment.state.annotatedString.text.indexOf("Row3")
        selectRange(viewModel, segmentId, row3Start, row3Start + "Row3".length)

        viewModel.onEvent(NoteEditorEvent.HeadingSelected(HeadingLevel.H1))

        val persisted = notesRepository.allNotes.single().document.blocks.map { it as NoteBlock.Paragraph }
        assertThat(persisted[0].heading).isNull()
        assertThat(persisted[1].heading).isEqualTo(HeadingLevel.H2)
        assertThat(persisted[2].heading).isEqualTo(HeadingLevel.H1)
    }

    // Regression test for the reported bug: pressing Enter at the end of a heading line reset the
    // new paragraph's HeadingStyle to Normal (library-native), but left the heading's bold+oversized
    // SpanStyle as the type-ahead style, so text typed right after Enter still rendered like a
    // heading. SegmentContentChanged must strip that residue once the paragraph is no longer heading.
    @Test
    fun `typing style resets to normal after pressing Enter at the end of a heading`() = runTest {
        val headingParagraph = NoteBlock.Paragraph(id = "p1", text = "Title", heading = HeadingLevel.H1)
        seedNote(note(document = NoteDocument(blocks = listOf(headingParagraph))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))
        val segment = viewModel.uiState.value.segments.single() as EditorSegment.Rich
        val endOfText = segment.state.annotatedString.text.length
        selectRange(viewModel, segmentId, endOfText, endOfText)

        segment.state.addTextAfterSelection("\n")
        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segmentId))

        assertThat(segment.state.currentHeadingStyle).isEqualTo(HeadingStyle.Normal)
        assertThat(segment.state.currentSpanStyle.fontSize).isEqualTo(TextUnit.Unspecified)
        assertThat(segment.state.currentSpanStyle.fontWeight).isNotEqualTo(FontWeight.Bold)

        segment.state.addTextAfterSelection("normal text")
        viewModel.onEvent(NoteEditorEvent.SegmentContentChanged(segmentId))
        advanceUntilIdle()

        val persisted = notesRepository.allNotes.single().document.blocks.map { it as NoteBlock.Paragraph }
        assertThat(persisted).hasSize(2)
        assertThat(persisted[1].heading).isNull()
        assertThat(persisted[1].text).isEqualTo("normal text")
    }

    @Test
    fun `heading toggle preserves the caret position`() = runTest {
        seedNote(note(document = NoteDocument(blocks = listOf(NoteBlock.Paragraph(id = "p1", text = "Title")))))
        val viewModel = viewModel()
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
        val segmentId = viewModel.uiState.value.segments.single().id
        viewModel.onEvent(NoteEditorEvent.SegmentFocusChanged(segmentId))
        selectRange(viewModel, segmentId, 3, 3)

        viewModel.onEvent(NoteEditorEvent.HeadingSelected(HeadingLevel.H1))

        val segment = viewModel.uiState.value.segments.single() as EditorSegment.Rich
        assertThat(segment.state.selection.min).isEqualTo(3)
        assertThat(segment.state.selection.max).isEqualTo(3)
    }
}
