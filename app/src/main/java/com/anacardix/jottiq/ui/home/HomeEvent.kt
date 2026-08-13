package com.anacardix.jottiq.ui.home

import com.anacardix.jottiq.domain.SortOrder

/** Intents for [HomeScreen]. */
sealed interface HomeEvent {
    data object ScreenShown : HomeEvent
    data object SortMenuOpened : HomeEvent
    data object SortMenuDismissed : HomeEvent
    data class SortOrderSelected(val order: SortOrder) : HomeEvent
    data object FabMenuToggled : HomeEvent
    data object FabMenuDismissed : HomeEvent
    data object CreateNoteClicked : HomeEvent
    data object CreateFolderClicked : HomeEvent
    data class CreateFolderNameChanged(val name: String) : HomeEvent
    data object CreateFolderConfirmed : HomeEvent
    data object CreateFolderDialogDismissed : HomeEvent
    data class FolderClicked(val folderId: String) : HomeEvent
    data class NoteClicked(val noteId: String) : HomeEvent
    data class NoteSwipedToDelete(val noteId: String) : HomeEvent
    data class NoteSwipedToFavorite(val noteId: String) : HomeEvent
    data class FolderSwipedToDelete(val folderId: String) : HomeEvent
    data class UndoDeleteClicked(val targetId: String, val isFolder: Boolean) : HomeEvent
    data object TrashClicked : HomeEvent
    data object SettingsClicked : HomeEvent
    data object UserMessageShown : HomeEvent
}
