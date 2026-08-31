package com.anacardix.jottiq.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.anacardix.jottiq.R
import com.anacardix.jottiq.designsystem.JottiqHapticType
import com.anacardix.jottiq.designsystem.JottiqSpacing
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.anacardix.jottiq.designsystem.rememberJottiqHaptics

/** Sentinel [MoveFolderRowUi.id] for "Notes (top level)" — a note's real `folderId` is `null` there. */
const val ROOT_FOLDER_ID = ""

/** One row of the Move-to-folder sheet (`design/10. Move to folder.png`). */
@Immutable
data class MoveFolderRowUi(
    val id: String,
    val name: String,
    val depth: Int,
    val isLocked: Boolean,
    val isCurrent: Boolean,
)

/**
 * Folder-picker bottom sheet shared by the single-note move action (Note Editor) and the
 * multi-select bulk move action (Home/Folder) — callback-based rather than tied to any one
 * screen's `Event` type, same reasoning as [SelectionActions].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderSheet(
    folders: List<MoveFolderRowUi>,
    selectedFolderId: String?,
    onFolderSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val haptics = rememberJottiqHaptics()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.move_sheet_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = JottiqSpacing.xl, vertical = JottiqSpacing.m),
        )
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            folders.forEach { row ->
                MoveFolderRow(
                    row = row,
                    isSelected = row.id == selectedFolderId,
                    onClick = { onFolderSelected(row.id) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(JottiqSpacing.l),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.move_sheet_cancel))
            }
            Button(
                onClick = {
                    haptics.perform(JottiqHapticType.Confirm)
                    onConfirm()
                },
                enabled = selectedFolderId != null,
                modifier = Modifier.padding(start = JottiqSpacing.s),
            ) {
                Text(stringResource(R.string.move_sheet_move))
            }
        }
    }
}

@Composable
private fun MoveFolderRow(row: MoveFolderRowUi, isSelected: Boolean, onClick: () -> Unit) {
    val background = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val name = if (row.id == ROOT_FOLDER_ID) stringResource(R.string.move_sheet_top_level) else row.name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .then(if (row.isCurrent) Modifier else Modifier.clickable(onClick = onClick))
            .padding(
                start = JottiqSpacing.xl + JottiqSpacing.xl * row.depth,
                end = JottiqSpacing.xl,
                top = JottiqSpacing.m,
                bottom = JottiqSpacing.m,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JottiqSpacing.l),
    ) {
        val glyph = when {
            row.id == ROOT_FOLDER_ID -> AppIcons.Inventory2
            row.isLocked -> AppIcons.Lock
            else -> AppIcons.Folder
        }
        AppIcon(glyph, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (row.isCurrent) {
            Text(
                text = stringResource(R.string.move_sheet_current),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
