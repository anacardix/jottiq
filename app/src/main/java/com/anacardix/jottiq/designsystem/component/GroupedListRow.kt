package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.clickable
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
 */
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
    colors: ListItemColors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
) {
    val shape = groupedRowShape(index, count)
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ListItem(
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = colors,
        modifier = modifier.clip(shape).then(clickableModifier),
    )
}
