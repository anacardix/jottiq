package com.anacardix.jottiq.ui.folder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.Folder
import com.anacardix.jottiq.domain.FoldersRepository
import com.anacardix.jottiq.domain.NoteSummary
import com.anacardix.jottiq.domain.NotesRepository
import com.anacardix.jottiq.domain.SettingsRepository
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.toLocale
import com.anacardix.jottiq.domain.usecase.CountNotesInSubtreeUseCase
import com.anacardix.jottiq.domain.usecase.FormatRelativeDateUseCase
import com.anacardix.jottiq.domain.usecase.GroupNotesByDateUseCase
import com.anacardix.jottiq.domain.usecase.SortFoldersUseCase
import com.anacardix.jottiq.domain.usecase.SortNotesUseCase
import com.anacardix.jottiq.security.LockSession
import com.anacardix.jottiq.ui.common.FolderRowUi
import com.anacardix.jottiq.ui.common.NoteRowUi
import com.anacardix.jottiq.ui.common.NoteSectionUi
import com.anacardix.jottiq.ui.common.UndoAction
import com.anacardix.jottiq.ui.common.UserMessage
import com.anacardix.jottiq.ui.navigation.FolderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions") // Hilt constructor injection; one handler per FolderEvent case
class FolderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val notesRepository: NotesRepository,
    private val foldersRepository: FoldersRepository,
    private val settingsRepository: SettingsRepository,
    private val countNotesInSubtree: CountNotesInSubtreeUseCase,
    private val formatRelativeDate: FormatRelativeDateUseCase,
    private val sortNotes: SortNotesUseCase,
    private val sortFolders: SortFoldersUseCase,
    private val groupNotesByDate: GroupNotesByDateUseCase,
    private val lockSession: LockSession,
) : ViewModel() {

    private val route: FolderRoute = savedStateHandle.toRoute()
    private val folderId: String = route.folderId

    private val _uiState = MutableStateFlow(FolderUiState(folderId = folderId))
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    private val navigationChannel = Channel<FolderNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<FolderNavigationEvent> = navigationChannel.receiveAsFlow()

    private var hasStartedObserving = false
    private val pendingNewNoteIds = MutableStateFlow<Set<String>>(emptySet())

    @Suppress("CyclomaticComplexMethod")
    fun onEvent(event: FolderEvent) {
        when (event) {
            FolderEvent.ScreenShown -> startObservingIfNeeded()
            FolderEvent.SortMenuOpened -> _uiState.update { it.copy(isSortMenuExpanded = true) }
            FolderEvent.SortMenuDismissed -> _uiState.update { it.copy(isSortMenuExpanded = false) }
            is FolderEvent.SortOrderSelected -> onSortOrderSelected(event.order)
            FolderEvent.FabMenuToggled ->
                _uiState.update { it.copy(isFabMenuExpanded = !it.isFabMenuExpanded) }
            FolderEvent.FabMenuDismissed -> _uiState.update { it.copy(isFabMenuExpanded = false) }
            FolderEvent.CreateNoteClicked -> onCreateNoteClicked()
            FolderEvent.CreateFolderClicked -> _uiState.update {
                it.copy(isFabMenuExpanded = false, isCreateFolderDialogVisible = true)
            }
            is FolderEvent.CreateFolderNameChanged ->
                _uiState.update { it.copy(createFolderName = event.name) }
            FolderEvent.CreateFolderConfirmed -> onCreateFolderConfirmed()
            FolderEvent.CreateFolderDialogDismissed -> _uiState.update {
                it.copy(isCreateFolderDialogVisible = false, createFolderName = "")
            }
            is FolderEvent.FolderClicked -> onFolderClicked(event.folderId)
            is FolderEvent.NoteClicked -> onNoteClicked(event.noteId)
            is FolderEvent.NoteSwipedToDelete -> onNoteSwipedToDelete(event.noteId)
            is FolderEvent.NoteSwipedToFavorite -> onNoteSwipedToFavorite(event.noteId)
            is FolderEvent.FolderSwipedToDelete -> onFolderSwipedToDelete(event.folderId)
            is FolderEvent.UndoDeleteClicked -> onUndoDeleteClicked(event.targetId, event.isFolder)
            FolderEvent.LockToggleClicked -> onLockToggleClicked()
            FolderEvent.UserMessageShown -> _uiState.update { it.copy(userMessage = null) }
        }
    }

    private fun startObservingIfNeeded() {
        if (hasStartedObserving) {
            // Returning to this screen (e.g. back from the note editor): stop hiding
            // whatever note we just created so it now shows up with its real content.
            pendingNewNoteIds.value = emptySet()
            return
        }
        hasStartedObserving = true
        combine(
            notesRepository.observeActiveNoteSummaries(),
            foldersRepository.observeActiveFolders(),
            settingsRepository.observeSortOrder(),
            lockSession.isUnlocked,
            pendingNewNoteIds,
        ) { notes, folders, sortOrder, isSessionUnlocked, pendingIds ->
            Snapshot(notes.filterNot { it.id in pendingIds }, folders, sortOrder, isSessionUnlocked)
        }
            .combine(settingsRepository.observeLanguage()) { snapshot, language ->
                snapshot.copy(locale = language.toLocale())
            }
            .onEach { applySnapshot(it) }
            .launchIn(viewModelScope)
    }

    private data class Snapshot(
        val notes: List<NoteSummary>,
        val folders: List<Folder>,
        val sortOrder: SortOrder,
        val isSessionUnlocked: Boolean,
        val locale: Locale = Locale.getDefault(),
    )

    private fun applySnapshot(snapshot: Snapshot) {
        val notes = snapshot.notes
        val folders = snapshot.folders
        val sortOrder = snapshot.sortOrder
        val isSessionUnlocked = snapshot.isSessionUnlocked
        val locale = snapshot.locale
        val self = folders.firstOrNull { it.id == folderId } ?: return
        val childFolders = folders.filter { it.parentId == folderId }
        val childNotes = notes.filter { it.folderId == folderId }
        val timestampOf: (NoteSummary) -> Long =
            if (sortOrder == SortOrder.DateCreated) NoteSummary::createdAt else NoteSummary::updatedAt
        val noteCounts = countNotesInSubtree(folders, notes)

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                folderName = self.name,
                isLocked = self.isLocked,
                itemCount = childFolders.size + childNotes.size,
                folders = sortFolders(childFolders, sortOrder).map { folder ->
                    FolderRowUi(
                        id = folder.id,
                        name = folder.name,
                        noteCount = noteCounts[folder.id] ?: 0,
                        isLocked = folder.isLocked,
                        isSessionUnlocked = isSessionUnlocked,
                    )
                },
                noteSections = groupNotesByDate(sortNotes(childNotes, sortOrder), locale, timestampOf)
                    .map { section ->
                        NoteSectionUi(section.group, section.notes.map { it.toRow(isSessionUnlocked, locale) })
                    },
                sortOrder = sortOrder,
            )
        }
    }

    private fun NoteSummary.toRow(isSessionUnlocked: Boolean, locale: Locale) = NoteRowUi(
        id = id,
        title = title,
        isFavorite = isFavorite,
        isLocked = isLocked,
        dateLabel = formatRelativeDate(updatedAt, locale),
        isSessionUnlocked = isSessionUnlocked,
    )

    private fun onFolderClicked(clickedFolderId: String) {
        val folder = _uiState.value.folders.firstOrNull { it.id == clickedFolderId } ?: return
        val event = if (folder.isLocked && !lockSession.isUnlocked.value) {
            FolderNavigationEvent.ToLockedFolder(clickedFolderId, folder.name)
        } else {
            FolderNavigationEvent.ToFolder(clickedFolderId)
        }
        navigationChannel.trySend(event)
    }

    private fun onNoteClicked(noteId: String) {
        val note = _uiState.value.noteSections.flatMap { it.notes }.firstOrNull { it.id == noteId } ?: return
        val event = if (note.isLocked && !lockSession.isUnlocked.value) {
            FolderNavigationEvent.ToLockedNote(noteId, note.title)
        } else {
            FolderNavigationEvent.ToNote(noteId)
        }
        navigationChannel.trySend(event)
    }

    private fun onSortOrderSelected(order: SortOrder) {
        _uiState.update { it.copy(isSortMenuExpanded = false) }
        viewModelScope.launch { settingsRepository.setSortOrder(order) }
    }

    private fun onCreateNoteClicked() {
        _uiState.update { it.copy(isFabMenuExpanded = false) }
        viewModelScope.launch {
            when (val result = notesRepository.createNote(folderId = folderId)) {
                is DataResult.Success -> {
                    pendingNewNoteIds.update { it + result.value.id }
                    navigationChannel.trySend(FolderNavigationEvent.ToNote(result.value.id))
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.home_error_create_note))
                }
            }
        }
    }

    private fun onCreateFolderConfirmed() {
        val name = _uiState.value.createFolderName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            when (foldersRepository.createFolder(parentId = folderId, name = name)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isCreateFolderDialogVisible = false, createFolderName = "")
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.home_error_create_folder))
                }
            }
        }
    }

    private fun onNoteSwipedToDelete(noteId: String) {
        viewModelScope.launch {
            when (notesRepository.moveToTrash(noteId)) {
                is DataResult.Success -> _uiState.update {
                    val undo = UndoAction(noteId, isFolder = false)
                    it.copy(userMessage = UserMessage(R.string.item_deleted_note, undo = undo))
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.error_delete_note))
                }
            }
        }
    }

    private fun onNoteSwipedToFavorite(noteId: String) {
        val note = _uiState.value.noteSections.flatMap { it.notes }.firstOrNull { it.id == noteId } ?: return
        viewModelScope.launch {
            when (notesRepository.setFavorite(noteId, !note.isFavorite)) {
                is DataResult.Success -> Unit
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.error_favorite_note))
                }
            }
        }
    }

    private fun onFolderSwipedToDelete(clickedFolderId: String) {
        viewModelScope.launch {
            when (foldersRepository.moveToTrash(clickedFolderId)) {
                is DataResult.Success -> _uiState.update {
                    val undo = UndoAction(clickedFolderId, isFolder = true)
                    it.copy(userMessage = UserMessage(R.string.item_deleted_folder, undo = undo))
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.error_delete_folder))
                }
            }
        }
    }

    private fun onUndoDeleteClicked(targetId: String, isFolder: Boolean) {
        viewModelScope.launch {
            val result = if (isFolder) {
                foldersRepository.restoreFromTrash(targetId)
            } else {
                notesRepository.restoreFromTrash(targetId)
            }
            when (result) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.error_undo))
                }
                is DataResult.Success -> _uiState.update { it.copy(undoNonce = it.undoNonce + 1) }
            }
        }
    }

    private fun onLockToggleClicked() {
        val lock = !_uiState.value.isLocked
        viewModelScope.launch {
            when (foldersRepository.setFolderLocked(folderId, lock)) {
                is DataResult.Success -> Unit
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.error_folder_lock))
                }
            }
        }
    }
}
