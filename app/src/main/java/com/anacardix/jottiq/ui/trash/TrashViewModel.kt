package com.anacardix.jottiq.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anacardix.jottiq.R
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.Folder
import com.anacardix.jottiq.domain.FoldersRepository
import com.anacardix.jottiq.domain.NoteSummary
import com.anacardix.jottiq.domain.NotesRepository
import com.anacardix.jottiq.domain.SettingsRepository
import com.anacardix.jottiq.domain.toLocale
import com.anacardix.jottiq.domain.usecase.CalculateTrashRetentionUseCase
import com.anacardix.jottiq.ui.common.UserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private val DELETED_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val foldersRepository: FoldersRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateTrashRetention: CalculateTrashRetentionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    private val navigationChannel = Channel<TrashNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<TrashNavigationEvent> = navigationChannel.receiveAsFlow()

    private var hasStartedObserving = false

    fun onEvent(event: TrashEvent) {
        when (event) {
            TrashEvent.ScreenShown -> startObservingIfNeeded()
            TrashEvent.BackClicked -> navigationChannel.trySend(TrashNavigationEvent.Back)
            TrashEvent.EmptyTrashClicked -> _uiState.update { it.copy(isEmptyTrashDialogVisible = true) }
            TrashEvent.EmptyTrashDialogDismissed -> _uiState.update { it.copy(isEmptyTrashDialogVisible = false) }
            TrashEvent.EmptyTrashConfirmed -> onEmptyTrashConfirmed()
            is TrashEvent.RestoreClicked -> onRestoreClicked(event.id)
            TrashEvent.UserMessageShown -> _uiState.update { it.copy(userMessage = null) }
        }
    }

    private fun startObservingIfNeeded() {
        if (hasStartedObserving) return
        hasStartedObserving = true
        combine(
            notesRepository.observeTrashedNoteSummaries(),
            foldersRepository.observeActiveFolders(),
            settingsRepository.observeLanguage(),
        ) { trashedNotes, activeFolders, language ->
            applySnapshot(trashedNotes, activeFolders, language.toLocale())
        }
            .launchIn(viewModelScope)
    }

    private fun applySnapshot(trashedNotes: List<NoteSummary>, activeFolders: List<Folder>, locale: Locale) {
        // Folders are containers, not recoverable items: they're soft-deleted alongside their notes
        // (still purged by Empty Trash / retention) but never shown as their own Trash row. A note's
        // folder name only resolves here if that folder is still active.
        val activeFoldersById = activeFolders.associateBy { it.id }
        val items = trashedNotes.mapNotNull { note ->
            val deletedAt = note.deletedAt ?: return@mapNotNull null
            deletedAt to TrashRowUi(
                id = note.id,
                title = note.title,
                folderName = activeFoldersById[note.folderId]?.name,
                deletedDateText = formatDeletedDate(deletedAt, locale),
                daysLeft = calculateTrashRetention(deletedAt),
            )
        }.sortedBy { (deletedAt, _) -> deletedAt }.map { (_, row) -> row }
        _uiState.update { it.copy(isLoading = false, items = items) }
    }

    private fun formatDeletedDate(deletedAt: Long, locale: Locale): String =
        DELETED_DATE_FORMAT.withLocale(locale).format(Instant.ofEpochMilli(deletedAt).atZone(clock.zone))

    private fun onRestoreClicked(id: String) {
        viewModelScope.launch {
            when (notesRepository.restoreFromTrash(id)) {
                is DataResult.Success -> Unit
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.trash_error))
                }
            }
        }
    }

    private fun onEmptyTrashConfirmed() {
        viewModelScope.launch {
            val notesResult = notesRepository.emptyTrash()
            val foldersResult = foldersRepository.emptyTrash()
            if (notesResult is DataResult.Success && foldersResult is DataResult.Success) {
                _uiState.update { it.copy(isEmptyTrashDialogVisible = false) }
            } else {
                _uiState.update {
                    it.copy(isEmptyTrashDialogVisible = false, userMessage = UserMessage(R.string.trash_error))
                }
            }
        }
    }
}
