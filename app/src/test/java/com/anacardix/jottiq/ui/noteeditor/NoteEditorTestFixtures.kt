package com.anacardix.jottiq.ui.noteeditor

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.SavedStateHandle
import com.anacardix.jottiq.domain.Note
import com.anacardix.jottiq.domain.NoteDocument
import com.anacardix.jottiq.domain.usecase.BuildFolderTreeUseCase
import com.anacardix.jottiq.domain.usecase.FormatRelativeDateUseCase
import com.anacardix.jottiq.fakes.FakeFoldersRepository
import com.anacardix.jottiq.fakes.FakeNotesRepository
import com.anacardix.jottiq.fakes.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Shared fixtures for [NoteEditorViewModelTest] and [NoteEditorStructuralEditsTest]. */
internal val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-07-17T14:02:00Z"), ZoneOffset.UTC)

@Suppress("LongParameterList") // exhaustive test-data builder mirroring every Note field
internal fun note(
    id: String = "note-1",
    title: String = "Groceries",
    document: NoteDocument = NoteDocument(),
    isFavorite: Boolean = false,
    isLocked: Boolean = false,
    folderId: String? = null,
) = Note(
    id = id,
    folderId = folderId,
    title = title,
    document = document,
    isFavorite = isFavorite,
    isLocked = isLocked,
    createdAt = 0L,
    updatedAt = 0L,
    deletedAt = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun testViewModel(
    notesRepository: FakeNotesRepository,
    foldersRepository: FakeFoldersRepository,
    settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    noteId: String = "note-1",
) = NoteEditorViewModel(
    savedStateHandle = SavedStateHandle(mapOf("noteId" to noteId)),
    notesRepository = notesRepository,
    foldersRepository = foldersRepository,
    settingsRepository = settingsRepository,
    formatRelativeDate = FormatRelativeDateUseCase(FIXED_CLOCK),
    buildFolderTree = BuildFolderTreeUseCase(),
    defaultDispatcher = UnconfinedTestDispatcher(),
)

internal fun selectRange(viewModel: NoteEditorViewModel, segmentId: String, start: Int, end: Int) {
    val segment = viewModel.uiState.value.segments.first { it.id == segmentId }
    segment.state.selection = TextRange(start, end)
}
