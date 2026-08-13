package com.anacardix.jottiq.ui.home

import app.cash.turbine.test
import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.DataError
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.usecase.CountNotesInSubtreeUseCase
import com.anacardix.jottiq.domain.usecase.FormatRelativeDateUseCase
import com.anacardix.jottiq.domain.usecase.GroupNotesByDateUseCase
import com.anacardix.jottiq.domain.usecase.NoteDateGroup
import com.anacardix.jottiq.domain.usecase.SortFoldersUseCase
import com.anacardix.jottiq.domain.usecase.SortNotesUseCase
import com.anacardix.jottiq.fakes.FakeFoldersRepository
import com.anacardix.jottiq.fakes.FakeNotesRepository
import com.anacardix.jottiq.fakes.FakeSettingsRepository
import com.anacardix.jottiq.fakes.folder
import com.anacardix.jottiq.fakes.note
import com.anacardix.jottiq.security.LockSession
import com.anacardix.jottiq.ui.common.FolderRowUi
import com.anacardix.jottiq.ui.common.UndoAction
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-07-17T14:02:00Z"), ZoneOffset.UTC)

/** Plain JVM unit test — the fakes and use cases under test touch no Android framework. */
class HomeViewModelTest {

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
        lockSession = lockSession,
    )

    @Test
    fun `initial state is loading`() = runTest {
        assertThat(viewModel().uiState.value.isLoading).isTrue()
    }

    @Test
    fun `ScreenShown loads top-level folders and notes with recursive counts`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "journal", name = "Journal", isLocked = true),
                folder(id = "personal", name = "Personal"),
                folder(id = "travel", parentId = "personal", name = "Travel"),
            ),
        )
        notesRepository.setNotes(
            listOf(
                note(id = "n1", folderId = "journal"),
                note(id = "n2", folderId = "journal"),
                note(id = "n3", folderId = "personal"),
                note(id = "n4", folderId = "travel"),
                note(id = "n5", folderId = null, title = "Groceries"),
            ),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()

            viewModel.onEvent(HomeEvent.ScreenShown)

            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.itemCount).isEqualTo(3) // 2 top-level folders + 1 top-level note
            assertThat(loaded.folders).containsExactly(
                FolderRowUi(id = "journal", name = "Journal", noteCount = 2, isLocked = true),
                FolderRowUi(id = "personal", name = "Personal", noteCount = 2, isLocked = false),
            )
            assertThat(loaded.noteSections.flatMap { it.notes }.single().id).isEqualTo("n5")
        }
    }

    @Test
    fun `favorite notes from any folder appear in the favorites section`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(
            listOf(
                note(id = "n1", folderId = "personal", title = "Kyoto itinerary", isFavorite = true),
                note(id = "n2", folderId = null, title = "Groceries", isFavorite = true),
                note(id = "n3", folderId = null, title = "Gift ideas", isFavorite = false),
            ),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(HomeEvent.ScreenShown)

            val loaded = awaitItem()
            assertThat(loaded.favoriteNotes.map { it.id }).containsExactly("n1", "n2")
        }
    }

    @Test
    fun `selecting a sort order persists it through settings and resorts the lists`() = runTest {
        notesRepository.setNotes(
            listOf(
                note(id = "b", title = "Banana", createdAt = 1L, updatedAt = 1L),
                note(id = "a", title = "Apple", createdAt = 2L, updatedAt = 2L),
            ),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(HomeEvent.ScreenShown)
            awaitItem()

            viewModel.onEvent(HomeEvent.SortOrderSelected(SortOrder.TitleAsc))

            val resorted = awaitItem()
            assertThat(resorted.sortOrder).isEqualTo(SortOrder.TitleAsc)
            assertThat(resorted.noteSections.flatMap { it.notes }.map { it.title })
                .containsExactly("Apple", "Banana").inOrder()
        }
    }

    @Test
    fun `notes are grouped into date sections ordered most-recent-first`() = runTest {
        val today = Instant.parse("2026-07-17T10:00:00Z")
        val tenDaysAgo = Instant.parse("2026-07-07T10:00:00Z")
        val lastYear = Instant.parse("2025-03-01T10:00:00Z")
        notesRepository.setNotes(
            listOf(
                note(id = "n1", title = "Today note", updatedAt = today.toEpochMilli()),
                note(id = "n2", title = "Ten days ago note", updatedAt = tenDaysAgo.toEpochMilli()),
                note(id = "n3", title = "Last year note", updatedAt = lastYear.toEpochMilli()),
            ),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(HomeEvent.ScreenShown)

            val loaded = awaitItem()
            assertThat(loaded.noteSections.map { it.group }).containsExactly(
                NoteDateGroup.Today,
                NoteDateGroup.Previous30Days,
                NoteDateGroup.Year("2025"),
            ).inOrder()
            assertThat(loaded.noteSections.map { section -> section.notes.map { it.id } }).containsExactly(
                listOf("n1"),
                listOf("n2"),
                listOf("n3"),
            ).inOrder()
        }
    }

    @Test
    fun `creating a note navigates to the new note`() = runTest {
        val viewModel = viewModel()

        viewModel.navigationEvents.test {
            viewModel.onEvent(HomeEvent.CreateNoteClicked)

            val event = awaitItem()
            assertThat(event).isInstanceOf(HomeNavigationEvent.ToNote::class.java)
        }
    }

    @Test
    fun `failed note creation surfaces a user message instead of navigating`() = runTest {
        notesRepository.createNoteFailure = DataError.Unknown
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(HomeEvent.CreateNoteClicked)

            val withMessage = awaitItem()
            assertThat(withMessage.userMessage?.messageResId).isEqualTo(R.string.home_error_create_note)
        }
    }

    @Test
    fun `confirming the create-folder dialog creates the folder and closes the dialog`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(HomeEvent.CreateFolderClicked)
            assertThat(awaitItem().isCreateFolderDialogVisible).isTrue()

            viewModel.onEvent(HomeEvent.CreateFolderNameChanged("Recipes"))
            assertThat(awaitItem().createFolderName).isEqualTo("Recipes")

            viewModel.onEvent(HomeEvent.CreateFolderConfirmed)
            val closed = awaitItem()
            assertThat(closed.isCreateFolderDialogVisible).isFalse()
            assertThat(closed.createFolderName).isEmpty()
        }
        assertThat(foldersRepository.allFolders.single().name).isEqualTo("Recipes")
    }

    @Test
    fun `confirming with a blank name does not create a folder`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.CreateFolderClicked)
        viewModel.onEvent(HomeEvent.CreateFolderNameChanged("   "))

        viewModel.onEvent(HomeEvent.CreateFolderConfirmed)

        assertThat(foldersRepository.allFolders).isEmpty()
        assertThat(viewModel.uiState.value.isCreateFolderDialogVisible).isTrue()
    }

    @Test
    fun `folder click emits a navigate-to-folder event`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "journal", name = "Journal")))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(HomeEvent.FolderClicked("journal"))

            assertThat(awaitItem()).isEqualTo(HomeNavigationEvent.ToFolder("journal"))
        }
    }

    @Test
    fun `clicking a locked folder navigates to the unlock gate instead`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "journal", name = "Journal", isLocked = true)))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(HomeEvent.FolderClicked("journal"))

            assertThat(awaitItem()).isEqualTo(HomeNavigationEvent.ToLockedFolder("journal", "Journal"))
        }
    }

    @Test
    fun `clicking a locked note navigates to the unlock gate instead`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", title = "Gift ideas", isLocked = true)))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(HomeEvent.NoteClicked("n1"))

            assertThat(awaitItem()).isEqualTo(HomeNavigationEvent.ToLockedNote("n1", "Gift ideas"))
        }
    }

    @Test
    fun `clicking a locked folder navigates straight through once the session is unlocked`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "journal", name = "Journal", isLocked = true)))
        lockSession.unlock()
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(HomeEvent.FolderClicked("journal"))

            assertThat(awaitItem()).isEqualTo(HomeNavigationEvent.ToFolder("journal"))
        }
    }

    @Test
    fun `clicking a locked note navigates straight through once the session is unlocked`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", title = "Gift ideas", isLocked = true)))
        lockSession.unlock()
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(HomeEvent.NoteClicked("n1"))

            assertThat(awaitItem()).isEqualTo(HomeNavigationEvent.ToNote("n1"))
        }
    }

    @Test
    fun `a locked folder row is session-unlocked when the session is already unlocked`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "journal", name = "Journal", isLocked = true)))
        lockSession.unlock()
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(HomeEvent.ScreenShown)

            assertThat(awaitItem().folders.single().isSessionUnlocked).isTrue()
        }
    }

    @Test
    fun `unlocking the session flips already-loaded locked rows to session-unlocked`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "journal", name = "Journal", isLocked = true)))
        notesRepository.setNotes(listOf(note(id = "n1", title = "Gift ideas", isLocked = true)))
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(HomeEvent.ScreenShown)

            val loaded = awaitItem()
            assertThat(loaded.folders.single().isSessionUnlocked).isFalse()
            assertThat(loaded.noteSections.flatMap { it.notes }.single().isSessionUnlocked).isFalse()

            lockSession.unlock()

            val unlocked = awaitItem()
            assertThat(unlocked.folders.single().isSessionUnlocked).isTrue()
            assertThat(unlocked.noteSections.flatMap { it.notes }.single().isSessionUnlocked).isTrue()
        }
    }

    @Test
    fun `trash and settings clicks emit their navigation events`() = runTest {
        val viewModel = viewModel()

        viewModel.navigationEvents.test {
            viewModel.onEvent(HomeEvent.TrashClicked)
            assertThat(awaitItem()).isEqualTo(HomeNavigationEvent.ToTrash)

            viewModel.onEvent(HomeEvent.SettingsClicked)
            assertThat(awaitItem()).isEqualTo(HomeNavigationEvent.ToSettings)
        }
    }

    @Test
    fun `UserMessageShown clears the pending message`() = runTest {
        notesRepository.createNoteFailure = DataError.Unknown
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(HomeEvent.CreateNoteClicked)
            assertThat(awaitItem().userMessage).isNotNull()

            viewModel.onEvent(HomeEvent.UserMessageShown)
            assertThat(awaitItem().userMessage).isNull()
        }
    }

    @Test
    fun `swiping a note to delete trashes it and offers an undo message`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", title = "Groceries")))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.onEvent(HomeEvent.NoteSwipedToDelete("n1"))

        assertThat(notesRepository.allNotes.single().deletedAt).isNotNull()
        val message = viewModel.uiState.value.userMessage
        assertThat(message?.messageResId).isEqualTo(R.string.item_deleted_note)
        assertThat(message?.undo).isEqualTo(UndoAction("n1", isFolder = false))
    }

    @Test
    fun `a failed note swipe-delete surfaces an error message`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1")))
        notesRepository.moveToTrashFailure = DataError.Unknown
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.onEvent(HomeEvent.NoteSwipedToDelete("n1"))

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_delete_note)
    }

    @Test
    fun `swiping a non-favorite note to favorite marks it favorite`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", title = "Groceries", isFavorite = false)))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.onEvent(HomeEvent.NoteSwipedToFavorite("n1"))

        assertThat(notesRepository.allNotes.single().isFavorite).isTrue()
        assertThat(viewModel.uiState.value.userMessage).isNull()
    }

    @Test
    fun `swiping an already-favorite note to favorite clears it`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", title = "Groceries", isFavorite = true)))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.onEvent(HomeEvent.NoteSwipedToFavorite("n1"))

        assertThat(notesRepository.allNotes.single().isFavorite).isFalse()
    }

    @Test
    fun `swiping a note to favorite does not touch its updatedAt`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1", title = "Groceries", updatedAt = 42L)))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.onEvent(HomeEvent.NoteSwipedToFavorite("n1"))

        assertThat(notesRepository.allNotes.single().updatedAt).isEqualTo(42L)
    }

    @Test
    fun `a failed favorite swipe surfaces an error message`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1")))
        notesRepository.setFavoriteFailure = DataError.Unknown
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.onEvent(HomeEvent.NoteSwipedToFavorite("n1"))

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_favorite_note)
    }

    @Test
    fun `swiping an unknown note id to favorite is a no-op`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.onEvent(HomeEvent.NoteSwipedToFavorite("missing"))

        assertThat(viewModel.uiState.value.userMessage).isNull()
    }

    @Test
    fun `swiping a folder to delete trashes it and offers an undo message`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "journal", name = "Journal")))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.onEvent(HomeEvent.FolderSwipedToDelete("journal"))

        assertThat(foldersRepository.allFolders.single().deletedAt).isNotNull()
        val message = viewModel.uiState.value.userMessage
        assertThat(message?.messageResId).isEqualTo(R.string.item_deleted_folder)
        assertThat(message?.undo).isEqualTo(UndoAction("journal", isFolder = true))
    }

    @Test
    fun `a failed folder swipe-delete surfaces an error message`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "journal", name = "Journal")))
        foldersRepository.moveToTrashFailure = DataError.Unknown
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)

        viewModel.onEvent(HomeEvent.FolderSwipedToDelete("journal"))

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_delete_folder)
    }

    @Test
    fun `undoing a note delete restores it and bumps the undo nonce`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1")))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.NoteSwipedToDelete("n1"))
        val nonceBeforeUndo = viewModel.uiState.value.undoNonce

        viewModel.onEvent(HomeEvent.UndoDeleteClicked("n1", isFolder = false))

        assertThat(notesRepository.allNotes.single().deletedAt).isNull()
        assertThat(viewModel.uiState.value.undoNonce).isEqualTo(nonceBeforeUndo + 1)
    }

    @Test
    fun `undoing a folder delete restores it`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "journal", name = "Journal")))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.FolderSwipedToDelete("journal"))

        viewModel.onEvent(HomeEvent.UndoDeleteClicked("journal", isFolder = true))

        assertThat(foldersRepository.allFolders.single().deletedAt).isNull()
    }

    @Test
    fun `a failed undo surfaces an error message and does not bump the undo nonce`() = runTest {
        notesRepository.setNotes(listOf(note(id = "n1")))
        val viewModel = viewModel()
        viewModel.onEvent(HomeEvent.ScreenShown)
        viewModel.onEvent(HomeEvent.NoteSwipedToDelete("n1"))
        val nonceBeforeUndo = viewModel.uiState.value.undoNonce
        notesRepository.restoreFromTrashFailure = DataError.Unknown

        viewModel.onEvent(HomeEvent.UndoDeleteClicked("n1", isFolder = false))

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_undo)
        assertThat(viewModel.uiState.value.undoNonce).isEqualTo(nonceBeforeUndo)
    }
}
