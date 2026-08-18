package com.anacardix.jottiq.ui.folder

import com.anacardix.jottiq.domain.SortOrder

/** Intents for [FolderScreen]. */
sealed interface FolderEvent {
    data object ScreenShown : FolderEvent
    data object SortMenuOpened : FolderEvent
    data object SortMenuDismissed : FolderEvent
    data class SortOrderSelected(val order: SortOrder) : FolderEvent
    data object FabMenuToggled : FolderEvent
    data object FabMenuDismissed : FolderEvent
    data object CreateNoteClicked : FolderEvent
    data object CreateFolderClicked : FolderEvent
    data class CreateFolderNameChanged(val name: String) : FolderEvent
    data object CreateFolderConfirmed : FolderEvent
    data object CreateFolderDialogDismissed : FolderEvent
    data class FolderClicked(val folderId: String) : FolderEvent
    data class NoteClicked(val noteId: String) : FolderEvent
    data class NoteSwipedToDelete(val noteId: String) : FolderEvent
    data class NoteSwipedToFavorite(val noteId: String) : FolderEvent
    data class FolderSwipedToDelete(val folderId: String) : FolderEvent
    data class UndoDeleteClicked(val noteIds: List<String>, val folderIds: List<String>) : FolderEvent
    data object LockToggleClicked : FolderEvent
    data object UserMessageShown : FolderEvent

    // Multi-select (long-press a row to enter, tap to toggle further rows).
    data class ItemLongPressed(val id: String, val isFolder: Boolean) : FolderEvent
    data class SelectionToggled(val id: String, val isFolder: Boolean) : FolderEvent
    data object SelectAllClicked : FolderEvent
    data object SelectionCancelled : FolderEvent
    data object DeleteSelectedClicked : FolderEvent
    data object FavoriteSelectedClicked : FolderEvent
}
