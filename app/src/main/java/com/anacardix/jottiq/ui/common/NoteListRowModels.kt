package com.anacardix.jottiq.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.anacardix.jottiq.domain.usecase.NoteDateGroup
import com.anacardix.jottiq.domain.usecase.RelativeDateLabel

/**
 * A folder row shared by Home's top-level list and Folder-view's subfolder list: leading folder
 * glyph, optional lock, recursive note count, chevron. [isSessionUnlocked] mirrors
 * [com.anacardix.jottiq.security.LockSession.isUnlocked] at snapshot time — it only changes how a
 * locked row's icon reads (open vs. closed padlock), never [isLocked] itself.
 */
@Immutable
data class FolderRowUi(
    val id: String,
    val name: String,
    val noteCount: Int,
    val isLocked: Boolean,
    val isSessionUnlocked: Boolean = false,
)

/** A note row shared by Home's Favorites/loose-notes sections and Folder-view's note list. */
@Immutable
data class NoteRowUi(
    val id: String,
    val title: String,
    val isFavorite: Boolean,
    val isLocked: Boolean,
    val dateLabel: RelativeDateLabel,
    val isSessionUnlocked: Boolean = false,
)

/** A [NoteDateGroup] header and the [NoteRowUi]s under it, in display order (Home's loose notes). */
@Immutable
data class NoteSectionUi(val group: NoteDateGroup, val notes: List<NoteRowUi>)

/**
 * A one-off, dismissible message (e.g. a failed create) surfaced via [androidx.compose.material3.Snackbar].
 * [undo], when present, adds an "Undo" action to the snackbar (e.g. after a swipe-to-delete).
 *
 * [messageResId] is a plain string resource id, unless [quantity] is set — then it's a `<plurals>`
 * resource id instead (e.g. a bulk multi-select delete's "N items moved to Trash"), resolved via
 * [androidx.compose.ui.res.pluralStringResource] with [quantity] as both the plural selector and its
 * `%d` format arg, matching this app's existing plural call sites (see `HomeScreen`'s item-count
 * subtitle). Not [androidx.annotation.StringRes]-annotated since it can hold either resource type.
 */
@Immutable
data class UserMessage(
    val messageResId: Int,
    val formatArgs: List<String> = emptyList(),
    val undo: UndoAction? = null,
    val quantity: Int? = null,
    val id: Long = System.nanoTime(),
)

/**
 * What a [UserMessage]'s "Undo" action restores: the trashed note/folder ids, split by which
 * repository owns them. A single swipe-to-delete fills exactly one list with one id; a bulk
 * multi-select delete can fill both.
 */
@Immutable
data class UndoAction(val noteIds: List<String> = emptyList(), val folderIds: List<String> = emptyList())

@Suppress("SpreadOperator") // formatArgs is always tiny (0-1 items) — not perf-sensitive
@Composable
fun UserMessage.resolve(): String {
    val quantity = quantity
    return if (quantity != null) {
        pluralStringResource(messageResId, quantity, quantity)
    } else {
        stringResource(messageResId, *formatArgs.toTypedArray())
    }
}
