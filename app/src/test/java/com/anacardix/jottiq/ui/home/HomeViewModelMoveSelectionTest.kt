package com.anacardix.jottiq.ui.home

import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.DataError
import com.anacardix.jottiq.domain.usecase.BuildFolderTreeUseCase
import com.anacardix.jottiq.domain.usecase.CollectFolderSubtreeIdsUseCase
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-07-17T14:02:00Z"), ZoneOffset.UTC)

/** Bulk "Move to folder" on a note/folder selection — split out of [HomeViewModelTest] (LargeClass). */
class HomeViewModelMoveSelectionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notesRepository = FakeNotesRepository()
    private val foldersRepository = FakeFoldersRepository()
    private val settingsRepository = FakeSettingsRepository()
    private val lockSession = LockSession()

    private fun viewModel() = HomeViewModel(
        notesRepository = notesRepository,
        foldersRepository = foldersRepository,
        settingsRepository = settingsRepository,
        countNotesInSubtree = CountNotesInSubtreeUseCase(),
        formatRelativeDate = FormatRelativeDateUseCase(FIXED_CLOCK),
        sortNotes = SortNotesUseCase(),
        sortFolders = SortFoldersUseCase(),
        groupNotesByDate = GroupNotesByDateUseCase(FIXED_CLOCK),
        buildFolderTree = BuildFolderTreeUseCase(),
        collectSubtreeIds = CollectFolderSubtreeIdsUseCase(),
        lockSession = lockSession,
    )

    @Test
    fun `MoveSelectedClicked loads the folder tree for the selected notes`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1")))
        foldersRepository.setFolders(
            listOf(folder(id = "personal", name = "Personal"), folder(id = "work", name = "Work")),
        )
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.ItemLongPressed("n1", isFolder = false))

        viewModel.onEvent(HomeEvent.MoveSelectedClicked)

        val state = viewModel.uiState.value
        assertThat(state.isMoveSheetVisible).isTrue()
        assertThat(state.moveFolders.map { it.id }).containsExactly("", "personal", "work")
        // Selected notes can span multiple folders, so no row is singled out as "current".
        assertThat(state.moveFolders.none { it.isCurrent }).isTrue()
    }

    @Test
    fun `selecting a folder and confirming moves every selected note and exits selection mode`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1"), note(id = "n2")))
        foldersRepository.setFolders(listOf(folder(id = "work", name = "Work")))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(HomeEvent.SelectionToggled("n2", isFolder = false))
        viewModel.onEvent(HomeEvent.MoveSelectedClicked)

        viewModel.onEvent(HomeEvent.MoveSelectionFolderSelected("work"))
        viewModel.onEvent(HomeEvent.MoveSelectionConfirmed)

        assertThat(notesRepository.allNotes.map { it.folderId }).containsExactly("work", "work")
        val state = viewModel.uiState.value
        assertThat(state.isMoveSheetVisible).isFalse()
        assertThat(state.selectionMode).isFalse()
        assertThat(state.selectedNoteIds).isEmpty()
    }

    @Test
    fun `selecting top level and confirming clears the selected notes' folder`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(HomeEvent.MoveSelectedClicked)

        viewModel.onEvent(HomeEvent.MoveSelectionFolderSelected(""))
        viewModel.onEvent(HomeEvent.MoveSelectionConfirmed)

        assertThat(notesRepository.allNotes.single().folderId).isNull()
    }

    @Test
    fun `a failed bulk move surfaces an error message`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1")))
        foldersRepository.setFolders(listOf(folder(id = "work", name = "Work")))
        notesRepository.setFolderFailure = DataError.Unknown
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(HomeEvent.MoveSelectedClicked)
        viewModel.onEvent(HomeEvent.MoveSelectionFolderSelected("work"))

        viewModel.onEvent(HomeEvent.MoveSelectionConfirmed)

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_move_selection)
    }

    @Test
    fun `MoveSelectionSheetDismissed hides the sheet without moving the selected notes`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1")))
        foldersRepository.setFolders(listOf(folder(id = "work", name = "Work")))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(HomeEvent.MoveSelectedClicked)
        viewModel.onEvent(HomeEvent.MoveSelectionFolderSelected("work"))

        viewModel.onEvent(HomeEvent.MoveSelectionSheetDismissed)

        assertThat(viewModel.uiState.value.isMoveSheetVisible).isFalse()
        assertThat(notesRepository.allNotes.single().folderId).isNull()
        assertThat(viewModel.uiState.value.selectionMode).isTrue()
    }

    @Test
    fun `MoveSelectedClicked excludes a selected folder and its whole subtree from the picker`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "travel", parentId = "personal", name = "Travel"),
                folder(id = "work", name = "Work"),
            ),
        )
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.ItemLongPressed("personal", isFolder = true))

        viewModel.onEvent(HomeEvent.MoveSelectedClicked)

        val state = viewModel.uiState.value
        assertThat(state.isMoveSheetVisible).isTrue()
        // "personal" (moving into itself) and "travel" (its descendant, a cycle) are both excluded;
        // root and the unrelated "work" folder stay pickable.
        assertThat(state.moveFolders.map { it.id }).containsExactly("", "work")
    }

    @Test
    fun `selecting a folder and confirming moves every selected folder and exits selection mode`() = runTest {
        foldersRepository.setFolders(
            listOf(folder(id = "personal", name = "Personal"), folder(id = "work", name = "Work")),
        )
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.ItemLongPressed("personal", isFolder = true))
        viewModel.onEvent(HomeEvent.MoveSelectedClicked)

        viewModel.onEvent(HomeEvent.MoveSelectionFolderSelected("work"))
        viewModel.onEvent(HomeEvent.MoveSelectionConfirmed)

        assertThat(foldersRepository.allFolders.first { it.id == "personal" }.parentId).isEqualTo("work")
        val state = viewModel.uiState.value
        assertThat(state.isMoveSheetVisible).isFalse()
        assertThat(state.selectionMode).isFalse()
        assertThat(state.selectedFolderIds).isEmpty()
    }

    @Test
    fun `confirming a mixed note and folder selection moves both`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1")))
        foldersRepository.setFolders(
            listOf(folder(id = "personal", name = "Personal"), folder(id = "work", name = "Work")),
        )
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(HomeEvent.SelectionToggled("personal", isFolder = true))
        viewModel.onEvent(HomeEvent.MoveSelectedClicked)

        viewModel.onEvent(HomeEvent.MoveSelectionFolderSelected("work"))
        viewModel.onEvent(HomeEvent.MoveSelectionConfirmed)

        assertThat(notesRepository.allNotes.single().folderId).isEqualTo("work")
        assertThat(foldersRepository.allFolders.first { it.id == "personal" }.parentId).isEqualTo("work")
    }

    @Test
    fun `a failed bulk folder move surfaces an error message`() = runTest {
        foldersRepository.setFolders(
            listOf(folder(id = "personal", name = "Personal"), folder(id = "work", name = "Work")),
        )
        foldersRepository.setParentFailure = DataError.Unknown
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.ItemLongPressed("personal", isFolder = true))
        viewModel.onEvent(HomeEvent.MoveSelectedClicked)
        viewModel.onEvent(HomeEvent.MoveSelectionFolderSelected("work"))

        viewModel.onEvent(HomeEvent.MoveSelectionConfirmed)

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_move_selection)
    }

    @Test
    fun `MoveSelectedClicked opens the sheet for a folder-only selection`() = runTest {
        foldersRepository.setFolders(
            listOf(folder(id = "personal", name = "Personal"), folder(id = "work", name = "Work")),
        )
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.ItemLongPressed("personal", isFolder = true))

        viewModel.onEvent(HomeEvent.MoveSelectedClicked)

        assertThat(viewModel.uiState.value.isMoveSheetVisible).isTrue()
    }
}
