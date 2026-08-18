package com.anacardix.jottiq.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
@Suppress("LongParameterList", "TooManyFunctions") // Hilt constructor injection; one handler per HomeEvent case
class HomeViewModel @Inject constructor(
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

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val navigationChannel = Channel<HomeNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<HomeNavigationEvent> = navigationChannel.receiveAsFlow()

    private var hasStartedObserving = false
    private val pendingNewNoteIds = MutableStateFlow<Set<String>>(emptySet())

    @Suppress("CyclomaticComplexMethod")
    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.ScreenShown -> startObservingIfNeeded()
            HomeEvent.SortMenuOpened -> _uiState.update { it.copy(isSortMenuExpanded = true) }
            HomeEvent.SortMenuDismissed -> _uiState.update { it.copy(isSortMenuExpanded = false) }
            is HomeEvent.SortOrderSelected -> onSortOrderSelected(event.order)
            HomeEvent.FabMenuToggled ->
                _uiState.update { it.copy(isFabMenuExpanded = !it.isFabMenuExpanded) }
            HomeEvent.FabMenuDismissed -> _uiState.update { it.copy(isFabMenuExpanded = false) }
            HomeEvent.CreateNoteClicked -> onCreateNoteClicked()
            HomeEvent.CreateFolderClicked -> _uiState.update {
                it.copy(isFabMenuExpanded = false, isCreateFolderDialogVisible = true)
            }
            is HomeEvent.CreateFolderNameChanged ->
                _uiState.update { it.copy(createFolderName = event.name) }
            HomeEvent.CreateFolderConfirmed -> onCreateFolderConfirmed()
            HomeEvent.CreateFolderDialogDismissed -> _uiState.update {
                it.copy(isCreateFolderDialogVisible = false, createFolderName = "")
            }
            is HomeEvent.FolderClicked -> onFolderClicked(event.folderId)
            is HomeEvent.NoteClicked -> onNoteClicked(event.noteId)
            is HomeEvent.NoteSwipedToDelete -> onNoteSwipedToDelete(event.noteId)
            is HomeEvent.NoteSwipedToFavorite -> onNoteSwipedToFavorite(event.noteId)
            is HomeEvent.FolderSwipedToDelete -> onFolderSwipedToDelete(event.folderId)
            is HomeEvent.UndoDeleteClicked -> onUndoDeleteClicked(event.noteIds, event.folderIds)
            HomeEvent.TrashClicked -> navigationChannel.trySend(HomeNavigationEvent.ToTrash)
            HomeEvent.SettingsClicked -> navigationChannel.trySend(HomeNavigationEvent.ToSettings)
            HomeEvent.UserMessageShown -> _uiState.update { it.copy(userMessage = null) }
            is HomeEvent.ItemLongPressed -> onItemLongPressed(event.id, event.isFolder)
            is HomeEvent.SelectionToggled -> onSelectionToggled(event.id, event.isFolder)
            HomeEvent.SelectAllClicked -> onSelectAllClicked()
            HomeEvent.SelectionCancelled -> onSelectionCancelled()
            HomeEvent.DeleteSelectedClicked -> onDeleteSelectedClicked()
            HomeEvent.FavoriteSelectedClicked -> onFavoriteSelectedClicked()
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
        val topLevelFolders = folders.filter { it.parentId == null }
        val topLevelNotes = notes.filter { it.folderId == null }
        val favorites = notes.filter { it.isFavorite }
        val timestampOf: (NoteSummary) -> Long =
            if (sortOrder == SortOrder.DateCreated) NoteSummary::createdAt else NoteSummary::updatedAt
        val noteCounts = countNotesInSubtree(folders, notes)
        // A note/folder can vanish out from under an active selection (e.g. trashed from another
        // screen/device in a future sync world); pruning to what's still visible keeps the selection
        // toolbar's count honest instead of counting ghosts.
        val visibleNoteIds = (favorites.map { it.id } + topLevelNotes.map { it.id }).toSet()
        val visibleFolderIds = topLevelFolders.map { it.id }.toSet()

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                itemCount = topLevelFolders.size + topLevelNotes.size,
                favoriteNotes = sortNotes(favorites, sortOrder).map { it.toRow(isSessionUnlocked, locale) },
                folders = sortFolders(topLevelFolders, sortOrder).map { folder ->
                    FolderRowUi(
                        id = folder.id,
                        name = folder.name,
                        noteCount = noteCounts[folder.id] ?: 0,
                        isLocked = folder.isLocked,
                        isSessionUnlocked = isSessionUnlocked,
                    )
                },
                noteSections = groupNotesByDate(sortNotes(topLevelNotes, sortOrder), locale, timestampOf)
                    .map { section ->
                        NoteSectionUi(section.group, section.notes.map { it.toRow(isSessionUnlocked, locale) })
                    },
                sortOrder = sortOrder,
                selectedNoteIds = current.selectedNoteIds.intersect(visibleNoteIds),
                selectedFolderIds = current.selectedFolderIds.intersect(visibleFolderIds),
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

    private fun onFolderClicked(folderId: String) {
        val folder = _uiState.value.folders.firstOrNull { it.id == folderId } ?: return
        val event = if (folder.isLocked && !lockSession.isUnlocked.value) {
            HomeNavigationEvent.ToLockedFolder(folderId, folder.name)
        } else {
            HomeNavigationEvent.ToFolder(folderId)
        }
        navigationChannel.trySend(event)
    }

    private fun onNoteClicked(noteId: String) {
        val state = _uiState.value
        val allNotes = state.favoriteNotes + state.noteSections.flatMap { it.notes }
        val note = allNotes.firstOrNull { it.id == noteId } ?: return
        val event = if (note.isLocked && !lockSession.isUnlocked.value) {
            HomeNavigationEvent.ToLockedNote(noteId, note.title)
        } else {
            HomeNavigationEvent.ToNote(noteId)
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
            when (val result = notesRepository.createNote(folderId = null)) {
                is DataResult.Success -> {
                    pendingNewNoteIds.update { it + result.value.id }
                    navigationChannel.trySend(HomeNavigationEvent.ToNote(result.value.id))
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
            when (foldersRepository.createFolder(parentId = null, name = name)) {
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
                    val undo = UndoAction(noteIds = listOf(noteId))
                    it.copy(userMessage = UserMessage(R.string.item_deleted_note, undo = undo))
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.error_delete_note))
                }
            }
        }
    }

    private fun onNoteSwipedToFavorite(noteId: String) {
        val state = _uiState.value
        val allNotes = state.favoriteNotes + state.noteSections.flatMap { it.notes }
        val note = allNotes.firstOrNull { it.id == noteId } ?: return
        viewModelScope.launch {
            when (notesRepository.setFavorite(noteId, !note.isFavorite)) {
                is DataResult.Success -> Unit
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.error_favorite_note))
                }
            }
        }
    }

    private fun onFolderSwipedToDelete(folderId: String) {
        viewModelScope.launch {
            when (foldersRepository.moveToTrash(folderId)) {
                is DataResult.Success -> _uiState.update {
                    val undo = UndoAction(folderIds = listOf(folderId))
                    it.copy(userMessage = UserMessage(R.string.item_deleted_folder, undo = undo))
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.error_delete_folder))
                }
            }
        }
    }

    private fun onUndoDeleteClicked(noteIds: List<String>, folderIds: List<String>) {
        viewModelScope.launch {
            val noteResult = if (noteIds.isNotEmpty()) notesRepository.restoreFromTrash(noteIds) else null
            val folderResult = if (folderIds.isNotEmpty()) foldersRepository.restoreFromTrash(folderIds) else null
            val failed = listOfNotNull(noteResult, folderResult).any { it is DataResult.Failure }
            if (failed) {
                _uiState.update { it.copy(userMessage = UserMessage(R.string.error_undo)) }
            } else {
                _uiState.update { it.copy(undoNonce = it.undoNonce + 1) }
            }
        }
    }

    private fun onItemLongPressed(id: String, isFolder: Boolean) {
        _uiState.update { state ->
            if (isFolder) {
                state.copy(selectionMode = true, selectedFolderIds = state.selectedFolderIds + id)
            } else {
                state.copy(selectionMode = true, selectedNoteIds = state.selectedNoteIds + id)
            }
        }
    }

    private fun onSelectionToggled(id: String, isFolder: Boolean) {
        _uiState.update { state ->
            val toggled = if (isFolder) {
                state.copy(selectedFolderIds = state.selectedFolderIds.toggled(id))
            } else {
                state.copy(selectedNoteIds = state.selectedNoteIds.toggled(id))
            }
            // Deselecting the last row exits selection mode, same as tapping Cancel.
            if (toggled.selectionCount == 0) toggled.copy(selectionMode = false) else toggled
        }
    }

    private fun onSelectAllClicked() {
        _uiState.update { state ->
            val allNoteIds = (state.favoriteNotes + state.noteSections.flatMap { it.notes }).map { it.id }.toSet()
            val allFolderIds = state.folders.map { it.id }.toSet()
            state.copy(selectedNoteIds = allNoteIds, selectedFolderIds = allFolderIds)
        }
    }

    private fun onSelectionCancelled() {
        _uiState.update {
            it.copy(selectionMode = false, selectedNoteIds = emptySet(), selectedFolderIds = emptySet())
        }
    }

    private fun onDeleteSelectedClicked() {
        val state = _uiState.value
        val noteIds = state.selectedNoteIds.toList()
        val folderIds = state.selectedFolderIds.toList()
        if (noteIds.isEmpty() && folderIds.isEmpty()) return
        viewModelScope.launch {
            val noteResult = if (noteIds.isNotEmpty()) notesRepository.moveToTrash(noteIds) else null
            val folderResult = if (folderIds.isNotEmpty()) foldersRepository.moveToTrash(folderIds) else null
            val failed = listOfNotNull(noteResult, folderResult).any { it is DataResult.Failure }
            if (failed) {
                _uiState.update { it.copy(userMessage = UserMessage(R.string.error_delete_selection)) }
            } else {
                val undo = UndoAction(noteIds = noteIds, folderIds = folderIds)
                val message = UserMessage(
                    R.plurals.selection_items_deleted,
                    undo = undo,
                    quantity = noteIds.size + folderIds.size,
                )
                _uiState.update {
                    it.copy(
                        selectionMode = false,
                        selectedNoteIds = emptySet(),
                        selectedFolderIds = emptySet(),
                        userMessage = message,
                    )
                }
            }
        }
    }

    private fun onFavoriteSelectedClicked() {
        val state = _uiState.value
        val noteIds = state.selectedNoteIds.toList()
        if (noteIds.isEmpty()) return
        val makeFavorite = !state.selectedNotesAllFavorite
        viewModelScope.launch {
            when (notesRepository.setFavorite(noteIds, makeFavorite)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(selectionMode = false, selectedNoteIds = emptySet(), selectedFolderIds = emptySet())
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.error_favorite_note))
                }
            }
        }
    }
}

private fun Set<String>.toggled(id: String): Set<String> = if (id in this) this - id else this + id
