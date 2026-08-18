package com.anacardix.jottiq.ui.home

import androidx.compose.runtime.Immutable
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.ui.common.FolderRowUi
import com.anacardix.jottiq.ui.common.NoteRowUi
import com.anacardix.jottiq.ui.common.NoteSectionUi
import com.anacardix.jottiq.ui.common.UserMessage

/** UI state for [HomeScreen], modeled on `design/01. Home.png`, `02. New.png`, `03. Sort.png`. */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val itemCount: Int = 0,
    val favoriteNotes: List<NoteRowUi> = emptyList(),
    val folders: List<FolderRowUi> = emptyList(),
    val noteSections: List<NoteSectionUi> = emptyList(),
    val sortOrder: SortOrder = SortOrder.DateEdited,
    val isSortMenuExpanded: Boolean = false,
    val isFabMenuExpanded: Boolean = false,
    val isCreateFolderDialogVisible: Boolean = false,
    val createFolderName: String = "",
    val userMessage: UserMessage? = null,
    // Bumped on every successful undo; forces swiped rows to reset their dismissed swipe state
    // (see SwipeableGroupedRow's kdoc) so a restored note/folder is never stuck rendered off-screen.
    val undoNonce: Int = 0,
    // Multi-select (long-press a row), enabling bulk favorite/unfavorite and bulk delete.
    val selectionMode: Boolean = false,
    val selectedNoteIds: Set<String> = emptySet(),
    val selectedFolderIds: Set<String> = emptySet(),
) {
    val isEmpty: Boolean get() = folders.isEmpty() && noteSections.isEmpty()

    val selectionCount: Int get() = selectedNoteIds.size + selectedFolderIds.size

    /** Drives the selection toolbar's Favorite/Unfavorite label: unfavorite only if every selected
     * note is already a favorite, otherwise favorite the whole selection. */
    val selectedNotesAllFavorite: Boolean
        get() {
            if (selectedNoteIds.isEmpty()) return false
            val allNotes = favoriteNotes + noteSections.flatMap { it.notes }
            return selectedNoteIds.all { id -> allNotes.firstOrNull { it.id == id }?.isFavorite == true }
        }
}
