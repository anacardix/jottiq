package com.anacardix.jottiq.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.anacardix.jottiq.R
import com.anacardix.jottiq.designsystem.JottiqSpacing
import com.anacardix.jottiq.designsystem.component.GroupedListRow
import com.anacardix.jottiq.designsystem.component.SelectionIndicator
import com.anacardix.jottiq.designsystem.component.SwipeableGroupedRow
import com.anacardix.jottiq.designsystem.groupedRowShape
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.anacardix.jottiq.domain.usecase.NoteDateGroup
import com.anacardix.jottiq.domain.usecase.RelativeDateLabel

/**
 * Renders [folders] as one rounded card group (Home's top-level folders, Folder-view's subfolders),
 * one [LazyListScope] item per row so each can animate in/out and be swiped away to delete it.
 * [resetSignal] is forwarded to [SwipeableGroupedRow] — see its kdoc for why undoing a delete must
 * change it.
 *
 * When [selectionMode] is on, rows drop their swipe gesture and long-press entirely: tapping a row
 * calls [onToggleSelection] instead of [onClick], and the leading folder glyph is replaced by a
 * [SelectionIndicator] reflecting membership in [selectedIds]. Otherwise, long-pressing a row calls
 * [onLongPress] to enter selection mode (the caller is expected to select that row too).
 */
// One independent, well-named knob per row-group concern (data, click, swipe, long-press, and
// selection state); a wrapper object would just relocate the count.
@Suppress("LongParameterList")
fun LazyListScope.folderRowGroup(
    folders: List<FolderRowUi>,
    onClick: (String) -> Unit,
    onSwipeToDelete: (String) -> Unit,
    resetSignal: Any = Unit,
    selectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onLongPress: (String) -> Unit = {},
    onToggleSelection: (String) -> Unit = {},
) {
    itemsIndexed(folders, key = { _, folder -> folder.id }) { index, folder ->
        val isSelected = folder.id in selectedIds
        val row: @Composable () -> Unit = {
            GroupedListRow(
                index = index,
                count = folders.size,
                // Screen gutter lives here rather than in the LazyColumn's contentPadding, so this
                // row (and thus SwipeableGroupedRow's measured width) spans the full list width and
                // a committed swipe can slide it all the way off the physical screen.
                modifier = Modifier.padding(horizontal = JottiqSpacing.screenGutter),
                headlineContent = { Text(folder.name, style = MaterialTheme.typography.titleMedium) },
                leadingContent = { FolderRowLeading(selectionMode, isSelected) },
                trailingContent = { FolderRowTrailing(folder, selectionMode) },
                onClick = { if (selectionMode) onToggleSelection(folder.id) else onClick(folder.id) },
                onLongClick = if (selectionMode) null else ({ onLongPress(folder.id) }),
                selected = selectionMode && isSelected,
            )
        }
        if (selectionMode) {
            Box(modifier = Modifier.animateItem()) { row() }
        } else {
            SwipeableGroupedRow(
                shape = groupedRowShape(index, folders.size),
                onDelete = { onSwipeToDelete(folder.id) },
                modifier = Modifier.animateItem(),
                resetSignal = resetSignal,
            ) { row() }
        }
    }
}

/** [folderRowGroup] row's leading glyph: the folder icon normally, or a [SelectionIndicator] in selection mode. */
@Composable
private fun FolderRowLeading(selectionMode: Boolean, isSelected: Boolean) {
    if (selectionMode) {
        SelectionIndicator(selected = isSelected)
    } else {
        AppIcon(
            AppIcons.Folder,
            contentDescription = null,
            filled = true,
            tint = MaterialTheme.colorScheme.secondary,
        )
    }
}

/** [folderRowGroup] row's trailing content: optional lock glyph, recursive note count, and a chevron
 * (hidden in selection mode, where tapping toggles selection rather than navigating). */
@Composable
private fun FolderRowTrailing(folder: FolderRowUi, selectionMode: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JottiqSpacing.s),
    ) {
        if (folder.isLocked) {
            AppIcon(
                if (folder.isSessionUnlocked) AppIcons.LockOpen else AppIcons.Lock,
                contentDescription = stringResource(
                    if (folder.isSessionUnlocked) R.string.home_folder_unlocked else R.string.home_folder_locked,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                sizeSp = 18,
            )
        }
        Text(
            text = folder.noteCount.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!selectionMode) {
            AppIcon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Renders [notes] as one rounded card group, one [LazyListScope] item per row so each can animate
 * in/out, be swiped away to delete it, or swiped the other way to toggle [NoteRowUi.isFavorite] via
 * [onSwipeToFavorite]. [showFavoriteIcon] is `false` for Home's Favorites shortcut section (favorite
 * status is already implied by the section itself) and `true` everywhere else a favorite note can
 * appear. [keyPrefix] disambiguates row keys when the same note can legitimately appear in two
 * groups on the same screen (a top-level favorite note is listed in both Home's Favorites shortcut
 * and its main notes group) — plain note ids would collide as `LazyColumn` keys otherwise.
 * [resetSignal] is forwarded to [SwipeableGroupedRow] — see its kdoc for why undoing a delete must
 * change it.
 *
 * When [selectionMode] is on, rows drop their swipe gesture and long-press entirely: tapping a row
 * calls [onToggleSelection] instead of [onClick], and a leading [SelectionIndicator] reflecting
 * membership in [selectedIds] is shown ahead of the title. Otherwise, long-pressing a row calls
 * [onLongPress] to enter selection mode (the caller is expected to select that row too).
 */
// One independent, well-named knob per row-group concern (data, click, two swipe directions, key
// disambiguation, swipe-state reset, selection state); a wrapper object would just relocate the count.
@Suppress("LongParameterList")
fun LazyListScope.noteRowGroup(
    notes: List<NoteRowUi>,
    showFavoriteIcon: Boolean,
    onClick: (String) -> Unit,
    onSwipeToDelete: (String) -> Unit,
    onSwipeToFavorite: (String) -> Unit,
    keyPrefix: String = "note",
    resetSignal: Any = Unit,
    selectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onLongPress: (String) -> Unit = {},
    onToggleSelection: (String) -> Unit = {},
) {
    itemsIndexed(notes, key = { _, note -> "$keyPrefix-${note.id}" }) { index, note ->
        val isSelected = note.id in selectedIds
        val row: @Composable () -> Unit = {
            GroupedListRow(
                index = index,
                count = notes.size,
                // See folderRowGroup's comment: gutter lives on the row, not the LazyColumn.
                modifier = Modifier.padding(horizontal = JottiqSpacing.screenGutter),
                headlineContent = {
                    Text(
                        text = note.title.ifBlank { stringResource(R.string.untitled_note) },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = if (selectionMode) {
                    { SelectionIndicator(selected = isSelected) }
                } else {
                    null
                },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(JottiqSpacing.s),
                    ) {
                        NoteStatusIcon(note, showFavoriteIcon)
                        Text(
                            text = note.dateLabel.resolve(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = { if (selectionMode) onToggleSelection(note.id) else onClick(note.id) },
                onLongClick = if (selectionMode) null else ({ onLongPress(note.id) }),
                selected = selectionMode && isSelected,
            )
        }
        if (selectionMode) {
            Box(modifier = Modifier.animateItem()) { row() }
        } else {
            SwipeableGroupedRow(
                shape = groupedRowShape(index, notes.size),
                onDelete = { onSwipeToDelete(note.id) },
                modifier = Modifier.animateItem(),
                onToggleFavorite = { onSwipeToFavorite(note.id) },
                isFavorite = note.isFavorite,
                resetSignal = resetSignal,
            ) { row() }
        }
    }
}

/**
 * A note-list section header (Today/Yesterday/Previous 7 Days/.../a month or year label), shared by
 * Home's loose-notes list and Folder-view's note list — both group their notes with
 * [com.anacardix.jottiq.domain.usecase.GroupNotesByDateUseCase].
 */
@Composable
fun NoteSectionLabel(group: NoteDateGroup) {
    Text(
        text = group.resolve(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = JottiqSpacing.screenGutter)
            .padding(bottom = JottiqSpacing.s),
    )
}

@Composable
private fun NoteDateGroup.resolve(): String = when (this) {
    NoteDateGroup.Today -> stringResource(R.string.home_date_today)
    NoteDateGroup.Yesterday -> stringResource(R.string.home_date_yesterday)
    NoteDateGroup.Previous7Days -> stringResource(R.string.home_date_previous_7_days)
    NoteDateGroup.Previous30Days -> stringResource(R.string.home_date_previous_30_days)
    is NoteDateGroup.Month -> label
    is NoteDateGroup.Year -> label
}

/** The lock/favorite glyph in a note row's trailing content — lock takes priority over favorite. */
@Composable
private fun NoteStatusIcon(note: NoteRowUi, showFavoriteIcon: Boolean) {
    when {
        note.isLocked -> AppIcon(
            if (note.isSessionUnlocked) AppIcons.LockOpen else AppIcons.Lock,
            contentDescription = stringResource(
                if (note.isSessionUnlocked) R.string.home_note_unlocked else R.string.home_note_locked,
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            sizeSp = 18,
        )
        note.isFavorite && showFavoriteIcon -> AppIcon(
            AppIcons.Star,
            contentDescription = stringResource(R.string.home_note_favorite),
            filled = true,
            tint = MaterialTheme.colorScheme.primary,
            sizeSp = 18,
        )
    }
}

@Composable
private fun RelativeDateLabel.resolve(): String = when (this) {
    is RelativeDateLabel.Time -> text
    is RelativeDateLabel.Date -> text
}
