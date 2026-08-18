package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.anacardix.jottiq.designsystem.groupedRowShape

/**
 * One row of a rounded card group (Home/Folder-view/Trash's note & folder lists): a stock M3
 * [ListItem] clipped to [groupedRowShape] and colored `surfaceContainerLow` per
 * `design/design-tokens.png`. Callers arrange consecutive rows with
 * `Arrangement.spacedBy(JottiqSpacing.groupGap)` so the 5dp inner corners read as one card.
 *
 * [onLongClick], when supplied alongside [onClick], enters the row's multi-select gesture (e.g.
 * Home/Folder/Trash's long-press-to-select). [selected] tints the row `secondaryContainer` instead
 * of the default `surfaceContainerLow` while a multi-select is active and this row is checked.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupedListRow(
    index: Int,
    count: Int,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    colors: ListItemColors = ListItemDefaults.colors(
        containerColor = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ),
) {
    val shape = groupedRowShape(index, count)
    val interactionModifier = when {
        onClick != null && onLongClick != null ->
            Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        onClick != null -> Modifier.clickable(onClick = onClick)
        // Long-press-only row (Trash's rows have no plain-tap action outside selection mode, only
        // Restore/long-press-to-select): combinedClickable still needs a click handler, so give it a
        // no-op one rather than dropping the long-press entirely.
        onLongClick != null -> Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
        else -> Modifier
    }
    ListItem(
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = colors,
        modifier = modifier.clip(shape).then(interactionModifier),
    )
}
