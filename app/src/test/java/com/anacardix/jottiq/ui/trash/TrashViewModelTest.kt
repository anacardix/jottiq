package com.anacardix.jottiq.ui.trash

import app.cash.turbine.test
import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.DataError
import com.anacardix.jottiq.domain.usecase.CalculateTrashRetentionUseCase
import com.anacardix.jottiq.fakes.FakeFoldersRepository
import com.anacardix.jottiq.fakes.FakeNotesRepository
import com.anacardix.jottiq.fakes.FakeSettingsRepository
import com.anacardix.jottiq.fakes.folder
import com.anacardix.jottiq.fakes.note
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private val FIXED_ZONE = ZoneOffset.UTC
private val NOW = Instant.parse("2026-07-17T14:02:00Z")
private const val SECONDS_PER_DAY = 24L * 60 * 60

@RunWith(RobolectricTestRunner::class)
class TrashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notesRepository = FakeNotesRepository()
    private val foldersRepository = FakeFoldersRepository()
    private val settingsRepository = FakeSettingsRepository()
    private val clock = Clock.fixed(NOW, FIXED_ZONE)

    private fun viewModel() = TrashViewModel(
        notesRepository = notesRepository,
        foldersRepository = foldersRepository,
        settingsRepository = settingsRepository,
        calculateTrashRetention = CalculateTrashRetentionUseCase(clock),
        clock = clock,
    )

    @Test
    fun `initial state is loading`() = runTest {
        assertThat(viewModel().uiState.value.isLoading).isTrue()
    }

    @Test
    fun `ScreenShown loads trashed notes ordered soonest-to-expire first`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "work", name = "Work")))
        notesRepository.setNotes(
            listOf(
                note(
                    id = "n1",
                    folderId = "work",
                    title = "Untitled note",
                    deletedAt = NOW.minusSeconds(2 * SECONDS_PER_DAY).toEpochMilli(),
                ),
                note(
                    id = "n2",
                    folderId = null,
                    title = "Old shopping list",
                    deletedAt = NOW.minusSeconds(24 * SECONDS_PER_DAY).toEpochMilli(),
                ),
                note(id = "n3", title = "Still active", deletedAt = null),
            ),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()

            viewModel.onEvent(TrashEvent.ScreenShown)

            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.items.map { it.id }).containsExactly("n2", "n1").inOrder()
            val oldShoppingList = loaded.items.first { it.id == "n2" }
            assertThat(oldShoppingList.folderName).isNull()
            assertThat(oldShoppingList.daysLeft).isEqualTo(6)
            val untitled = loaded.items.first { it.id == "n1" }
            assertThat(untitled.folderName).isEqualTo("Work")
            assertThat(untitled.daysLeft).isEqualTo(28)
        }
    }

    @Test
    fun `RestoreClicked restores the note`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)

        viewModel.onEvent(TrashEvent.RestoreClicked("n1"))

        assertThat(notesRepository.allNotes.single().deletedAt).isNull()
        assertThat(viewModel.uiState.value.userMessage).isNull()
    }

    @Test
    fun `a failed restore surfaces an error message`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        notesRepository.restoreFromTrashFailure = DataError.Unknown
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)

        viewModel.onEvent(TrashEvent.RestoreClicked("n1"))

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.trash_error)
    }

    @Test
    fun `ScreenShown never lists trashed folders as their own row, only their notes`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "work", name = "Work"),
                folder(
                    id = "sketches",
                    parentId = "work",
                    name = "Sketches",
                    deletedAt = NOW.minusSeconds(3 * SECONDS_PER_DAY).toEpochMilli(),
                ),
            ),
        )
        notesRepository.setNotes(
            listOf(note(id = "n1", title = "Old list", deletedAt = NOW.minusSeconds(SECONDS_PER_DAY).toEpochMilli())),
        )
        val viewModel = viewModel()

        viewModel.onEvent(TrashEvent.ScreenShown)

        val items = viewModel.uiState.value.items
        assertThat(items.map { it.id }).containsExactly("n1")
    }

    @Test
    fun `EmptyTrashClicked opens the confirmation dialog without deleting`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)

        viewModel.onEvent(TrashEvent.EmptyTrashClicked)

        assertThat(viewModel.uiState.value.isEmptyTrashDialogVisible).isTrue()
        assertThat(notesRepository.allNotes).isNotEmpty()
    }

    @Test
    fun `EmptyTrashConfirmed hard-deletes every trashed note and folder, and closes the dialog`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        foldersRepository.setFolders(listOf(folder(id = "sketches", deletedAt = NOW.toEpochMilli())))
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)
        viewModel.onEvent(TrashEvent.EmptyTrashClicked)

        viewModel.onEvent(TrashEvent.EmptyTrashConfirmed)

        assertThat(viewModel.uiState.value.isEmptyTrashDialogVisible).isFalse()
        assertThat(notesRepository.allNotes).isEmpty()
        assertThat(foldersRepository.allFolders).isEmpty()
    }

    @Test
    fun `EmptyTrashDialogDismissed closes the dialog without deleting`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)
        viewModel.onEvent(TrashEvent.EmptyTrashClicked)

        viewModel.onEvent(TrashEvent.EmptyTrashDialogDismissed)

        assertThat(viewModel.uiState.value.isEmptyTrashDialogVisible).isFalse()
        assertThat(notesRepository.allNotes).isNotEmpty()
    }

    @Test
    fun `BackClicked emits the Back navigation event`() = runTest {
        val viewModel = viewModel()

        viewModel.navigationEvents.test {
            viewModel.onEvent(TrashEvent.BackClicked)

            assertThat(awaitItem()).isEqualTo(TrashNavigationEvent.Back)
        }
    }

    @Test
    fun `long-pressing a row enters selection mode and selects it`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)

        viewModel.onEvent(TrashEvent.ItemLongPressed("n1"))

        val state = viewModel.uiState.value
        assertThat(state.selectionMode).isTrue()
        assertThat(state.selectedNoteIds).containsExactly("n1")
        assertThat(state.selectionCount).isEqualTo(1)
    }

    @Test
    fun `deselecting the last selected row exits selection mode`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)
        viewModel.onEvent(TrashEvent.ItemLongPressed("n1"))

        viewModel.onEvent(TrashEvent.SelectionToggled("n1"))

        val state = viewModel.uiState.value
        assertThat(state.selectionMode).isFalse()
        assertThat(state.selectedNoteIds).isEmpty()
    }

    @Test
    fun `SelectAllClicked selects every trashed row`() = runTest {
        notesRepository.setNotes(
            listOf(
                note(id = "n1", deletedAt = NOW.toEpochMilli()),
                note(id = "n2", deletedAt = NOW.toEpochMilli()),
            ),
        )
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)
        viewModel.onEvent(TrashEvent.ItemLongPressed("n1"))

        viewModel.onEvent(TrashEvent.SelectAllClicked)

        assertThat(viewModel.uiState.value.selectedNoteIds).containsExactly("n1", "n2")
    }

    @Test
    fun `SelectionCancelled exits selection mode and clears the selection`() = runTest {
        notesRepository.setNotes(
            listOf(
                note(id = "n1", deletedAt = NOW.toEpochMilli()),
                note(id = "n2", deletedAt = NOW.toEpochMilli()),
            ),
        )
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)
        viewModel.onEvent(TrashEvent.ItemLongPressed("n1"))
        viewModel.onEvent(TrashEvent.SelectionToggled("n2"))

        viewModel.onEvent(TrashEvent.SelectionCancelled)

        val state = viewModel.uiState.value
        assertThat(state.selectionMode).isFalse()
        assertThat(state.selectedNoteIds).isEmpty()
    }

    @Test
    fun `RestoreSelectedClicked restores every selected note and exits selection mode`() = runTest {
        notesRepository.setNotes(
            listOf(
                note(id = "n1", deletedAt = NOW.toEpochMilli()),
                note(id = "n2", deletedAt = NOW.toEpochMilli()),
            ),
        )
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)
        viewModel.onEvent(TrashEvent.ItemLongPressed("n1"))
        viewModel.onEvent(TrashEvent.SelectionToggled("n2"))

        viewModel.onEvent(TrashEvent.RestoreSelectedClicked)

        assertThat(notesRepository.allNotes.all { it.deletedAt == null }).isTrue()
        val state = viewModel.uiState.value
        assertThat(state.selectionMode).isFalse()
        assertThat(state.selectedNoteIds).isEmpty()
    }

    @Test
    fun `a failed bulk restore surfaces an error message`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        notesRepository.restoreFromTrashFailure = DataError.Unknown
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)
        viewModel.onEvent(TrashEvent.ItemLongPressed("n1"))

        viewModel.onEvent(TrashEvent.RestoreSelectedClicked)

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.trash_error)
    }

    @Test
    fun `DeleteForeverSelectedClicked opens the confirmation dialog without deleting`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)
        viewModel.onEvent(TrashEvent.ItemLongPressed("n1"))

        viewModel.onEvent(TrashEvent.DeleteForeverSelectedClicked)

        assertThat(viewModel.uiState.value.isDeleteForeverDialogVisible).isTrue()
        assertThat(notesRepository.allNotes).isNotEmpty()
    }

    @Test
    fun `DeleteForeverConfirmed hard-deletes every selected note, closes the dialog, and exits selection`() =
        runTest {
            notesRepository.setNotes(
                listOf(
                    note(id = "n1", deletedAt = NOW.toEpochMilli()),
                    note(id = "n2", deletedAt = NOW.toEpochMilli()),
                ),
            )
            val viewModel = viewModel()
            viewModel.onEvent(TrashEvent.ScreenShown)
            viewModel.onEvent(TrashEvent.ItemLongPressed("n1"))
            viewModel.onEvent(TrashEvent.DeleteForeverSelectedClicked)

            viewModel.onEvent(TrashEvent.DeleteForeverConfirmed)

            assertThat(notesRepository.allNotes.map { it.id }).containsExactly("n2")
            val state = viewModel.uiState.value
            assertThat(state.isDeleteForeverDialogVisible).isFalse()
            assertThat(state.selectionMode).isFalse()
            assertThat(state.selectedNoteIds).isEmpty()
        }

    @Test
    fun `DeleteForeverDialogDismissed closes the dialog without deleting`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)
        viewModel.onEvent(TrashEvent.ItemLongPressed("n1"))
        viewModel.onEvent(TrashEvent.DeleteForeverSelectedClicked)

        viewModel.onEvent(TrashEvent.DeleteForeverDialogDismissed)

        assertThat(viewModel.uiState.value.isDeleteForeverDialogVisible).isFalse()
        assertThat(notesRepository.allNotes).isNotEmpty()
    }

    @Test
    fun `a failed DeleteForeverConfirmed surfaces an error message and closes the dialog`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", deletedAt = NOW.toEpochMilli())))
        notesRepository.deleteForeverFailure = DataError.Unknown
        val viewModel = viewModel()
        viewModel.onEvent(TrashEvent.ScreenShown)
        viewModel.onEvent(TrashEvent.ItemLongPressed("n1"))
        viewModel.onEvent(TrashEvent.DeleteForeverSelectedClicked)

        viewModel.onEvent(TrashEvent.DeleteForeverConfirmed)

        val state = viewModel.uiState.value
        assertThat(state.userMessage?.messageResId).isEqualTo(R.string.trash_error)
        assertThat(state.isDeleteForeverDialogVisible).isFalse()
    }
}
