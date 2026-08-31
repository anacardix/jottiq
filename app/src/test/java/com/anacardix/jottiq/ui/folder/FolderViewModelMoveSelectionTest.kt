package com.anacardix.jottiq.ui.folder

import androidx.lifecycle.SavedStateHandle
import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.DataError
import com.anacardix.jottiq.domain.usecase.BuildFolderTreeUseCase
import com.anacardix.jottiq.domain.usecase.CountNotesInSubtreeUseCase
import com.anacardix.jottiq.domain.usecase.FormatRelativeDateUseCase
import com.anacardix.jottiq.domain.usecase.GroupNotesByDateUseCase
import com.anacardix.jottiq.domain.usecase.SortFoldersUseCase
import com.anacardix.jottiq.domain.usecase.SortNotesUseCase
import com.anacardix.jottiq.fakes.FakeFoldersRepository
import com.anacardix.jottiq.fakes.FakeNotesRepository
import com.anacardix.jottiq.fakes.FakeSettingsRepository
import com.anacardix.jottiq.fakes.folder
import com.anacardix.jottiq.fakes.note
import com.anacardix.jottiq.security.LockSession
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-07-17T14:02:00Z"), ZoneOffset.UTC)

/** Bulk "Move to folder" on a note selection — split out of [FolderViewModelTest] (LargeClass). */
@RunWith(RobolectricTestRunner::class)
class FolderViewModelMoveSelectionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notesRepository = FakeNotesRepository()
    private val foldersRepository = FakeFoldersRepository()
    private val settingsRepository = FakeSettingsRepository()
    private val lockSession = LockSession()

    private fun viewModel(folderId: String = "personal") = FolderViewModel(
        savedStateHandle = SavedStateHandle(mapOf("folderId" to folderId)),
        notesRepository = notesRepository,
        foldersRepository = foldersRepository,
        settingsRepository = settingsRepository,
        countNotesInSubtree = CountNotesInSubtreeUseCase(),
        formatRelativeDate = FormatRelativeDateUseCase(FIXED_CLOCK),
        sortNotes = SortNotesUseCase(),
        sortFolders = SortFoldersUseCase(),
        groupNotesByDate = GroupNotesByDateUseCase(FIXED_CLOCK),
        buildFolderTree = BuildFolderTreeUseCase(),
        lockSession = lockSession,
    )

    @Test
    fun `MoveSelectedClicked loads the folder tree for the selected notes`() = runTest {
        foldersRepository.setFolders(
            listOf(folder(id = "personal", name = "Personal"), folder(id = "work", name = "Work")),
        )
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))

        viewModel.onEvent(FolderEvent.MoveSelectedClicked)

        val state = viewModel.uiState.value
        assertThat(state.isMoveSheetVisible).isTrue()
        assertThat(state.moveFolders.map { it.id }).containsExactly("", "personal", "work")
        // No row is singled out as "current" for bulk move, unlike NoteEditor's single-note move.
        assertThat(state.moveFolders.none { it.isCurrent }).isTrue()
    }

    @Test
    fun `selecting a folder and confirming moves every selected note and exits selection mode`() = runTest {
        foldersRepository.setFolders(
            listOf(folder(id = "personal", name = "Personal"), folder(id = "work", name = "Work")),
        )
        notesRepository.setNotes(
            listOf(note(id = "n1", folderId = "personal"), note(id = "n2", folderId = "personal")),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(FolderEvent.SelectionToggled("n2", isFolder = false))
        viewModel.onEvent(FolderEvent.MoveSelectedClicked)

        viewModel.onEvent(FolderEvent.MoveSelectionFolderSelected("work"))
        viewModel.onEvent(FolderEvent.MoveSelectionConfirmed)

        assertThat(notesRepository.allNotes.map { it.folderId }).containsExactly("work", "work")
        val state = viewModel.uiState.value
        assertThat(state.isMoveSheetVisible).isFalse()
        assertThat(state.selectionMode).isFalse()
        assertThat(state.selectedNoteIds).isEmpty()
    }

    @Test
    fun `selecting top level and confirming clears the selected notes' folder`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(FolderEvent.MoveSelectedClicked)

        viewModel.onEvent(FolderEvent.MoveSelectionFolderSelected(""))
        viewModel.onEvent(FolderEvent.MoveSelectionConfirmed)

        assertThat(notesRepository.allNotes.single().folderId).isNull()
    }

    @Test
    fun `a failed bulk move surfaces an error message`() = runTest {
        foldersRepository.setFolders(
            listOf(folder(id = "personal", name = "Personal"), folder(id = "work", name = "Work")),
        )
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        notesRepository.setFolderFailure = DataError.Unknown
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(FolderEvent.MoveSelectedClicked)
        viewModel.onEvent(FolderEvent.MoveSelectionFolderSelected("work"))

        viewModel.onEvent(FolderEvent.MoveSelectionConfirmed)

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_move_selection)
    }

    @Test
    fun `MoveSelectionSheetDismissed hides the sheet without moving the selected notes`() = runTest {
        foldersRepository.setFolders(
            listOf(folder(id = "personal", name = "Personal"), folder(id = "work", name = "Work")),
        )
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(FolderEvent.MoveSelectedClicked)
        viewModel.onEvent(FolderEvent.MoveSelectionFolderSelected("work"))

        viewModel.onEvent(FolderEvent.MoveSelectionSheetDismissed)

        assertThat(viewModel.uiState.value.isMoveSheetVisible).isFalse()
        assertThat(notesRepository.allNotes.single().folderId).isEqualTo("personal")
        assertThat(viewModel.uiState.value.selectionMode).isTrue()
    }
}
