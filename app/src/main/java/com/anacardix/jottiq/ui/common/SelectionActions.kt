package com.anacardix.jottiq.ui.common

import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anacardix.jottiq.R
import com.anacardix.jottiq.designsystem.JottiqHapticType
import com.anacardix.jottiq.designsystem.component.SelectionTopBar
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.anacardix.jottiq.designsystem.rememberJottiqHaptics

/**
 * The bulk-action row Home/Folder put in [SelectionTopBar]'s `actions` slot: Select All, then
 * Favorite (toggling to Unfavorite once every selected note already is one — [allSelectedFavorite]),
 * Move to folder, both enabled only once a note is selected ([hasSelectedNotes]; folders are neither
 * favoritable nor moved by this action), then Delete.
 * Callback-based rather than tied to either screen's `Event` type — Home and Folder are separate
 * near-identical MVI screens (own `ViewModel`/`UiState`/`Event`), so this is shared as plain lambdas.
 */
@Composable
fun SelectionActions(
    hasSelectedNotes: Boolean,
    allSelectedFavorite: Boolean,
    onSelectAllClick: () -> Unit,
    onFavoriteToggleClick: () -> Unit,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val haptics = rememberJottiqHaptics()
    TextButton(onClick = onSelectAllClick) {
        Text(stringResource(R.string.selection_select_all))
    }
    IconButton(
        onClick = {
            haptics.perform(if (allSelectedFavorite) JottiqHapticType.ToggleOff else JottiqHapticType.ToggleOn)
            onFavoriteToggleClick()
        },
        enabled = hasSelectedNotes,
    ) {
        AppIcon(
            AppIcons.Star,
            contentDescription = stringResource(
                if (allSelectedFavorite) R.string.selection_unfavorite_action else R.string.selection_favorite_action,
            ),
            filled = allSelectedFavorite,
        )
    }
    IconButton(onClick = onMoveClick, enabled = hasSelectedNotes) {
        AppIcon(AppIcons.DriveFileMove, contentDescription = stringResource(R.string.selection_move_action))
    }
    IconButton(
        onClick = {
            haptics.perform(JottiqHapticType.Reject)
            onDeleteClick()
        },
    ) {
        AppIcon(
            AppIcons.Delete,
            contentDescription = stringResource(R.string.row_swipe_delete_action),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}
