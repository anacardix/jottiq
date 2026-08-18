package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.anacardix.jottiq.R
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIcons

/**
 * The top bar Home/Folder/Trash swap in for their normal [JottiqTopAppBar] while a multi-select is
 * active: a Close nav icon that cancels the selection (clearing it without acting), the selected
 * count as the title, and a caller-supplied [actions] row for the available bulk operations
 * (Favorite/Unfavorite + Delete on Home/Folder; Restore + Delete Forever on Trash).
 *
 * [subtitle] must be passed by callers whose normal top bar has one (Home/Folder's item count):
 * `LargeFlexibleTopAppBar` renders a shorter bar when there's no subtitle, so omitting it here would
 * shrink the bar's height on entering selection mode and jump the list content up by the difference.
 * Trash has no subtitle in its normal state, so it passes none here either.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    JottiqTopAppBar(
        title = { Text(pluralStringResource(R.plurals.selection_selected_count, selectedCount, selectedCount)) },
        subtitle = subtitle,
        navigationIcon = {
            IconButton(onClick = onCancelClick) {
                AppIcon(AppIcons.Close, contentDescription = stringResource(R.string.selection_cancel_action))
            }
        },
        actions = actions,
        modifier = modifier,
        scrollBehavior = scrollBehavior,
    )
}
