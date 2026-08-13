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
)

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
