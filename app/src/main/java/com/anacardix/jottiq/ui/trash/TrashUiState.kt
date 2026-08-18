package com.anacardix.jottiq.ui.trash

import androidx.compose.runtime.Immutable
import com.anacardix.jottiq.ui.common.UserMessage

/** UI state for [TrashScreen], modeled on `design/14. Trash.png`. */
@Immutable
data class TrashUiState(
    val isLoading: Boolean = true,
    val items: List<TrashRowUi> = emptyList(),
    val isEmptyTrashDialogVisible: Boolean = false,
    val userMessage: UserMessage? = null,
    // Multi-select (long-press a row), enabling bulk Restore and bulk Delete Forever. Trash shows
    // notes only (folders are containers, never their own trash row — see TrashViewModel's kdoc), so
    // there's no folder id set and no favorite action here, unlike Home/Folder's selection mode.
    val selectionMode: Boolean = false,
    val selectedNoteIds: Set<String> = emptySet(),
    val isDeleteForeverDialogVisible: Boolean = false,
) {
    val selectionCount: Int get() = selectedNoteIds.size
}

/**
 * One trashed note row. [folderName] is the note's containing folder, if that folder still exists
 * and is active; `null` for top-level notes and for notes whose folder is gone (the screen shows the
 * localized "Notes" root label for that case, same convention as the Move-to-folder sheet — this is
 * also where the note lands if restored).
 */
@Immutable
data class TrashRowUi(
    val id: String,
    val title: String,
    val folderName: String?,
    val deletedDateText: String,
    val daysLeft: Int,
)
