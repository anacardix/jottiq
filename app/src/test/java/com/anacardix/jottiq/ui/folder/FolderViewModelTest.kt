package com.anacardix.jottiq.ui.folder

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.DataError
import com.anacardix.jottiq.domain.SortOrder
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
import com.anacardix.jottiq.ui.common.FolderRowUi
import com.anacardix.jottiq.ui.common.UndoAction
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

@RunWith(RobolectricTestRunner::class)
class FolderViewModelTest {

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
        collectSubtreeIds = CollectFolderSubtreeIdsUseCase(),
        lockSession = lockSession,
    )

    @Test
    fun `initial state is loading and carries the requested folder id`() = runTest {
        val state = viewModel(folderId = "journal").uiState.value

        assertThat(state.isLoading).isTrue()
        assertThat(state.folderId).isEqualTo("journal")
    }

    @Test
    fun `ScreenShown loads this folder's direct children with recursive counts`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "travel", parentId = "personal", name = "Travel"),
                folder(id = "japan", parentId = "travel", name = "Japan 2026"),
            ),
        )
        notesRepository.setNotes(
            listOf(
                note(id = "n1", folderId = "personal", title = "Apartment ideas"),
                note(id = "n2", folderId = "travel"),
                note(id = "n3", folderId = "japan"),
            ),
        )
        val viewModel = viewModel(folderId = "personal")

        viewModel.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()

            viewModel.onEvent(FolderEvent.ScreenShown)

            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.folderName).isEqualTo("Personal")
            assertThat(loaded.itemCount).isEqualTo(2) // Travel folder + Apartment ideas note
            assertThat(loaded.folders).containsExactly(
                FolderRowUi(id = "travel", name = "Travel", noteCount = 2, isLocked = false),
            )
            assertThat(loaded.noteSections.single().notes.single().id).isEqualTo("n1")
        }
    }

    @Test
    fun `creating a note scopes it to the current folder and navigates to it`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        val viewModel = viewModel(folderId = "personal")

        viewModel.navigationEvents.test {
            viewModel.onEvent(FolderEvent.CreateNoteClicked)

            val event = awaitItem()
            assertThat(event).isInstanceOf(FolderNavigationEvent.ToNote::class.java)
        }
        assertThat(notesRepository.allNotes.single().folderId).isEqualTo("personal")
    }

    @Test
    fun `failed note creation surfaces a user message instead of navigating`() = runTest {
        notesRepository.createNoteFailure = DataError.Unknown
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        val viewModel = viewModel(folderId = "personal")

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(FolderEvent.CreateNoteClicked)

            val withMessage = awaitItem()
            assertThat(withMessage.userMessage?.messageResId).isEqualTo(R.string.home_error_create_note)
        }
    }

    @Test
    fun `confirming the create-folder dialog nests the new folder under this one`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        val viewModel = viewModel(folderId = "personal")

        viewModel.onEvent(FolderEvent.CreateFolderClicked)
        viewModel.onEvent(FolderEvent.CreateFolderNameChanged("Travel"))
        viewModel.onEvent(FolderEvent.CreateFolderConfirmed)

        val created = foldersRepository.allFolders.single { it.name == "Travel" }
        assertThat(created.parentId).isEqualTo("personal")
    }

    @Test
    fun `selecting a sort order persists it and resorts this folder's lists`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(
            listOf(
                note(id = "b", folderId = "personal", title = "Banana", createdAt = 1L, updatedAt = 1L),
                note(id = "a", folderId = "personal", title = "Apple", createdAt = 2L, updatedAt = 2L),
            ),
        )
        val viewModel = viewModel(folderId = "personal")

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(FolderEvent.ScreenShown)
            awaitItem()

            viewModel.onEvent(FolderEvent.SortOrderSelected(SortOrder.TitleAsc))

            val resorted = awaitItem()
            assertThat(resorted.sortOrder).isEqualTo(SortOrder.TitleAsc)
            assertThat(resorted.noteSections.single().notes.map { it.title })
                .containsExactly("Apple", "Banana").inOrder()
        }
    }

    @Test
    fun `folder click emits a navigate-to-folder event`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "travel", parentId = "personal", name = "Travel"),
            ),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(FolderEvent.FolderClicked("travel"))

            assertThat(awaitItem()).isEqualTo(FolderNavigationEvent.ToFolder("travel"))
        }
    }

    @Test
    fun `clicking a locked folder navigates to the unlock gate instead`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "journal", parentId = "personal", name = "Journal", isLocked = true),
            ),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(FolderEvent.FolderClicked("journal"))

            assertThat(awaitItem()).isEqualTo(FolderNavigationEvent.ToLockedFolder("journal", "Journal"))
        }
    }

    @Test
    fun `clicking a locked note navigates to the unlock gate instead`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(
            listOf(note(id = "n1", folderId = "personal", title = "Gift ideas", isLocked = true)),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(FolderEvent.NoteClicked("n1"))

            assertThat(awaitItem()).isEqualTo(FolderNavigationEvent.ToLockedNote("n1", "Gift ideas"))
        }
    }

    @Test
    fun `clicking a locked folder navigates straight through once the session is unlocked`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "journal", parentId = "personal", name = "Journal", isLocked = true),
            ),
        )
        lockSession.unlock()
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(FolderEvent.FolderClicked("journal"))

            assertThat(awaitItem()).isEqualTo(FolderNavigationEvent.ToFolder("journal"))
        }
    }

    @Test
    fun `clicking a locked note navigates straight through once the session is unlocked`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(
            listOf(note(id = "n1", folderId = "personal", title = "Gift ideas", isLocked = true)),
        )
        lockSession.unlock()
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.navigationEvents.test {
            viewModel.onEvent(FolderEvent.NoteClicked("n1"))

            assertThat(awaitItem()).isEqualTo(FolderNavigationEvent.ToNote("n1"))
        }
    }

    @Test
    fun `a locked child folder row is session-unlocked when the session is already unlocked`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "journal", parentId = "personal", name = "Journal", isLocked = true),
            ),
        )
        lockSession.unlock()
        val viewModel = viewModel(folderId = "personal")

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(FolderEvent.ScreenShown)

            assertThat(awaitItem().folders.single().isSessionUnlocked).isTrue()
        }
    }

    @Test
    fun `unlocking the session flips already-loaded locked rows to session-unlocked`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "journal", parentId = "personal", name = "Journal", isLocked = true),
            ),
        )
        notesRepository.setNotes(
            listOf(note(id = "n1", folderId = "personal", title = "Gift ideas", isLocked = true)),
        )
        val viewModel = viewModel(folderId = "personal")

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(FolderEvent.ScreenShown)

            val loaded = awaitItem()
            assertThat(loaded.folders.single().isSessionUnlocked).isFalse()
            assertThat(loaded.noteSections.single().notes.single().isSessionUnlocked).isFalse()

            lockSession.unlock()

            val unlocked = awaitItem()
            assertThat(unlocked.folders.single().isSessionUnlocked).isTrue()
            assertThat(unlocked.noteSections.single().notes.single().isSessionUnlocked).isTrue()
        }
    }

    @Test
    fun `LockToggleClicked locks an unlocked folder`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal", isLocked = false)))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.LockToggleClicked)

        assertThat(foldersRepository.allFolders.single().isLocked).isTrue()
        assertThat(viewModel.uiState.value.userMessage).isNull()
    }

    @Test
    fun `LockToggleClicked removes the lock from a locked folder`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal", isLocked = true)))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.LockToggleClicked)

        assertThat(foldersRepository.allFolders.single().isLocked).isFalse()
        assertThat(viewModel.uiState.value.userMessage).isNull()
    }

    @Test
    fun `LockToggleClicked cascades the lock to descendant folders`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal", isLocked = false),
                folder(id = "travel", parentId = "personal", name = "Travel", isLocked = false),
            ),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.LockToggleClicked)

        assertThat(foldersRepository.allFolders.first { it.id == "travel" }.isLocked).isTrue()
    }

    @Test
    fun `a failed lock toggle surfaces an error message`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        foldersRepository.setFolderLockedFailure = DataError.Unknown
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.LockToggleClicked)

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_folder_lock)
    }

    @Test
    fun `UserMessageShown clears the pending message`() = runTest {
        notesRepository.createNoteFailure = DataError.Unknown
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        val viewModel = viewModel(folderId = "personal")

        viewModel.uiState.test {
            awaitItem()
            viewModel.onEvent(FolderEvent.CreateNoteClicked)
            assertThat(awaitItem().userMessage).isNotNull()

            viewModel.onEvent(FolderEvent.UserMessageShown)
            assertThat(awaitItem().userMessage).isNull()
        }
    }

    @Test
    fun `swiping a note to delete trashes it and offers an undo message`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.NoteSwipedToDelete("n1"))

        assertThat(notesRepository.allNotes.single().deletedAt).isNotNull()
        val message = viewModel.uiState.value.userMessage
        assertThat(message?.messageResId).isEqualTo(R.string.item_deleted_note)
        assertThat(message?.undo).isEqualTo(UndoAction(noteIds = listOf("n1")))
    }

    @Test
    fun `swiping a non-favorite note to favorite marks it favorite`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal", isFavorite = false)))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.NoteSwipedToFavorite("n1"))

        assertThat(notesRepository.allNotes.single().isFavorite).isTrue()
        assertThat(viewModel.uiState.value.userMessage).isNull()
    }

    @Test
    fun `swiping an already-favorite note to favorite clears it`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal", isFavorite = true)))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.NoteSwipedToFavorite("n1"))

        assertThat(notesRepository.allNotes.single().isFavorite).isFalse()
    }

    @Test
    fun `a failed favorite swipe surfaces an error message`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        notesRepository.setFavoriteFailure = DataError.Unknown
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.NoteSwipedToFavorite("n1"))

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_favorite_note)
    }

    @Test
    fun `swiping a subfolder to delete trashes it and offers an undo message`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "travel", parentId = "personal", name = "Travel"),
            ),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.FolderSwipedToDelete("travel"))

        assertThat(foldersRepository.allFolders.first { it.id == "travel" }.deletedAt).isNotNull()
        val message = viewModel.uiState.value.userMessage
        assertThat(message?.messageResId).isEqualTo(R.string.item_deleted_folder)
        assertThat(message?.undo).isEqualTo(UndoAction(folderIds = listOf("travel")))
    }

    @Test
    fun `undoing a note delete restores it and bumps the undo nonce`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.NoteSwipedToDelete("n1"))
        val nonceBeforeUndo = viewModel.uiState.value.undoNonce

        viewModel.onEvent(FolderEvent.UndoDeleteClicked(noteIds = listOf("n1"), folderIds = emptyList()))

        assertThat(notesRepository.allNotes.single().deletedAt).isNull()
        assertThat(viewModel.uiState.value.undoNonce).isEqualTo(nonceBeforeUndo + 1)
    }

    @Test
    fun `a failed swipe-delete surfaces an error message`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        notesRepository.moveToTrashFailure = DataError.Unknown
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.NoteSwipedToDelete("n1"))

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_delete_note)
    }

    @Test
    fun `long-pressing a note enters selection mode and selects it`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)

        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))

        val state = viewModel.uiState.value
        assertThat(state.selectionMode).isTrue()
        assertThat(state.selectedNoteIds).containsExactly("n1")
        assertThat(state.selectionCount).isEqualTo(1)
    }

    @Test
    fun `toggling an unselected subfolder while in selection mode adds it to the selection`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "travel", parentId = "personal", name = "Travel"),
            ),
        )
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))

        viewModel.onEvent(FolderEvent.SelectionToggled("travel", isFolder = true))

        val state = viewModel.uiState.value
        assertThat(state.selectedNoteIds).containsExactly("n1")
        assertThat(state.selectedFolderIds).containsExactly("travel")
    }

    @Test
    fun `deselecting the last selected item exits selection mode`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))

        viewModel.onEvent(FolderEvent.SelectionToggled("n1", isFolder = false))

        val state = viewModel.uiState.value
        assertThat(state.selectionMode).isFalse()
        assertThat(state.selectedNoteIds).isEmpty()
    }

    @Test
    fun `SelectAllClicked selects every visible note and subfolder`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "travel", parentId = "personal", name = "Travel"),
            ),
        )
        notesRepository.setNotes(
            listOf(note(id = "n1", folderId = "personal"), note(id = "n2", folderId = "personal")),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))

        viewModel.onEvent(FolderEvent.SelectAllClicked)

        val state = viewModel.uiState.value
        assertThat(state.selectedNoteIds).containsExactly("n1", "n2")
        assertThat(state.selectedFolderIds).containsExactly("travel")
    }

    @Test
    fun `SelectionCancelled exits selection mode and clears the selection`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(
            listOf(note(id = "n1", folderId = "personal"), note(id = "n2", folderId = "personal")),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(FolderEvent.SelectionToggled("n2", isFolder = false))

        viewModel.onEvent(FolderEvent.SelectionCancelled)

        val state = viewModel.uiState.value
        assertThat(state.selectionMode).isFalse()
        assertThat(state.selectedNoteIds).isEmpty()
    }

    @Test
    fun `DeleteSelectedClicked trashes the selected notes and folders and offers a bulk undo message`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "travel", parentId = "personal", name = "Travel"),
            ),
        )
        notesRepository.setNotes(
            listOf(note(id = "n1", folderId = "personal"), note(id = "n2", folderId = "personal")),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(FolderEvent.SelectionToggled("travel", isFolder = true))

        viewModel.onEvent(FolderEvent.DeleteSelectedClicked)

        assertThat(notesRepository.allNotes.first { it.id == "n1" }.deletedAt).isNotNull()
        assertThat(notesRepository.allNotes.first { it.id == "n2" }.deletedAt).isNull()
        assertThat(foldersRepository.allFolders.first { it.id == "travel" }.deletedAt).isNotNull()
        val state = viewModel.uiState.value
        assertThat(state.selectionMode).isFalse()
        assertThat(state.selectedNoteIds).isEmpty()
        assertThat(state.selectedFolderIds).isEmpty()
        val message = state.userMessage
        assertThat(message?.messageResId).isEqualTo(R.plurals.selection_items_deleted)
        assertThat(message?.quantity).isEqualTo(2)
        assertThat(message?.undo).isEqualTo(UndoAction(noteIds = listOf("n1"), folderIds = listOf("travel")))
    }

    @Test
    fun `undoing a bulk delete restores both the notes and folders`() = runTest {
        foldersRepository.setFolders(
            listOf(
                folder(id = "personal", name = "Personal"),
                folder(id = "travel", parentId = "personal", name = "Travel"),
            ),
        )
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(FolderEvent.SelectionToggled("travel", isFolder = true))
        viewModel.onEvent(FolderEvent.DeleteSelectedClicked)

        viewModel.onEvent(FolderEvent.UndoDeleteClicked(noteIds = listOf("n1"), folderIds = listOf("travel")))

        assertThat(notesRepository.allNotes.single().deletedAt).isNull()
        assertThat(foldersRepository.allFolders.first { it.id == "travel" }.deletedAt).isNull()
    }

    @Test
    fun `a failed bulk delete surfaces an error message`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        notesRepository.moveToTrashFailure = DataError.Unknown
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))

        viewModel.onEvent(FolderEvent.DeleteSelectedClicked)

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_delete_selection)
    }

    @Test
    fun `FavoriteSelectedClicked favorites all selected notes when not every one is already favorite`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(
            listOf(
                note(id = "n1", folderId = "personal", isFavorite = false),
                note(id = "n2", folderId = "personal", isFavorite = true),
            ),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(FolderEvent.SelectionToggled("n2", isFolder = false))

        viewModel.onEvent(FolderEvent.FavoriteSelectedClicked)

        assertThat(notesRepository.allNotes.first { it.id == "n1" }.isFavorite).isTrue()
        assertThat(notesRepository.allNotes.first { it.id == "n2" }.isFavorite).isTrue()
        assertThat(viewModel.uiState.value.selectionMode).isFalse()
    }

    @Test
    fun `FavoriteSelectedClicked unfavorites every selected note when all are already favorite`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(
            listOf(
                note(id = "n1", folderId = "personal", isFavorite = true),
                note(id = "n2", folderId = "personal", isFavorite = true),
            ),
        )
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))
        viewModel.onEvent(FolderEvent.SelectionToggled("n2", isFolder = false))

        viewModel.onEvent(FolderEvent.FavoriteSelectedClicked)

        assertThat(notesRepository.allNotes.all { !it.isFavorite }).isTrue()
    }

    @Test
    fun `a failed bulk favorite surfaces an error message`() = runTest {
        foldersRepository.setFolders(listOf(folder(id = "personal", name = "Personal")))
        notesRepository.setNotes(listOf(note(id = "n1", folderId = "personal")))
        notesRepository.setFavoriteFailure = DataError.Unknown
        val viewModel = viewModel(folderId = "personal")
        viewModel.onEvent(FolderEvent.ScreenShown)
        viewModel.onEvent(FolderEvent.ItemLongPressed("n1", isFolder = false))

        viewModel.onEvent(FolderEvent.FavoriteSelectedClicked)

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.error_favorite_note)
    }
}
