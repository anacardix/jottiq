package com.anacardix.jottiq.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIcons

private val INDICATOR_DIAMETER = 24.dp
private val UNSELECTED_BORDER_WIDTH = 2.dp
private const val CHECK_ICON_SIZE_SP = 16

/**
 * The leading checkmark/empty-circle glyph a row shows in a list's multi-select mode (Home/Folder's
 * note & folder rows, Trash's note rows) — filled `primary` with a check when [selected], an outlined
 * circle otherwise. Purely a rendering toggle: the caller owns tap handling and the selection set.
 */
@Composable
fun SelectionIndicator(selected: Boolean, modifier: Modifier = Modifier) {
    if (selected) {
        Box(
            modifier = modifier
                .size(INDICATOR_DIAMETER)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                AppIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                sizeSp = CHECK_ICON_SIZE_SP,
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(INDICATOR_DIAMETER)
                .border(UNSELECTED_BORDER_WIDTH, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}
