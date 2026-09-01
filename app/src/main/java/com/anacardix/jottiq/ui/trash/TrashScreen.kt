package com.anacardix.jottiq.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anacardix.jottiq.R
import com.anacardix.jottiq.designsystem.JottiqHapticType
import com.anacardix.jottiq.designsystem.JottiqShapes
import com.anacardix.jottiq.designsystem.JottiqSpacing
import com.anacardix.jottiq.designsystem.JottiqTheme
import com.anacardix.jottiq.designsystem.component.EmptyStateView
import com.anacardix.jottiq.designsystem.component.GroupedListRow
import com.anacardix.jottiq.designsystem.component.JottiqLoadingIndicator
import com.anacardix.jottiq.designsystem.component.JottiqTopAppBar
import com.anacardix.jottiq.designsystem.component.SelectionIndicator
import com.anacardix.jottiq.designsystem.component.SelectionTopBar
import com.anacardix.jottiq.designsystem.component.rememberJottiqTopAppBarScrollBehavior
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.anacardix.jottiq.designsystem.rememberJottiqHaptics
import com.anacardix.jottiq.ui.common.resolve
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TrashScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(TrashEvent.ScreenShown)
    }
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                TrashNavigationEvent.Back -> onBackClick()
            }
        }
    }
    TrashContent(uiState = uiState, onEvent = viewModel::onEvent, onBackClick = onBackClick, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrashContent(
    uiState: TrashUiState,
    onEvent: (TrashEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage = uiState.userMessage
    val resolvedUserMessage = userMessage?.resolve()
    LaunchedEffect(userMessage?.id) {
        if (resolvedUserMessage != null) {
            try {
                snackbarHostState.showSnackbar(resolvedUserMessage)
            } finally {
                // Runs even if navigating away cancels this coroutine mid-snackbar, so the
                // message is always marked consumed and never re-shown on the next recomposition.
                onEvent(TrashEvent.UserMessageShown)
            }
        }
    }

    BackHandler(enabled = uiState.selectionMode) { onEvent(TrashEvent.SelectionCancelled) }

    val scrollBehavior = rememberJottiqTopAppBarScrollBehavior()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (uiState.selectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectionCount,
                    onCancelClick = { onEvent(TrashEvent.SelectionCancelled) },
                    actions = {
                        TrashSelectionActions(
                            hasSelection = uiState.selectedNoteIds.isNotEmpty(),
                            onEvent = onEvent,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            } else {
                JottiqTopAppBar(
                    title = stringResource(R.string.trash_title),
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            AppIcon(
                                AppIcons.ArrowBack,
                                contentDescription = stringResource(R.string.trash_back_action),
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onEvent(TrashEvent.EmptyTrashClicked) },
                            enabled = uiState.items.isNotEmpty(),
                        ) {
                            Text(
                                text = stringResource(R.string.trash_empty_action),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            TrashList(
                items = uiState.items,
                isLoading = uiState.isLoading,
                selectionMode = uiState.selectionMode,
                selectedIds = uiState.selectedNoteIds,
                onEvent = onEvent,
                contentPadding = innerPadding,
            )
            if (uiState.isLoading) {
                JottiqLoadingIndicator()
            }
        }
    }

    if (uiState.isEmptyTrashDialogVisible) {
        EmptyTrashDialog(onEvent = onEvent)
    }
    if (uiState.isDeleteForeverDialogVisible) {
        DeleteForeverDialog(onEvent = onEvent)
    }
}

/** The bulk-action row for [SelectionTopBar] on Trash: Select All, Restore, then Delete Forever
 * (opens [DeleteForeverDialog] — irreversible, unlike Home/Folder's straight-to-Trash bulk delete). */
@Composable
private fun TrashSelectionActions(hasSelection: Boolean, onEvent: (TrashEvent) -> Unit) {
    val haptics = rememberJottiqHaptics()
    TextButton(onClick = { onEvent(TrashEvent.SelectAllClicked) }) {
        Text(stringResource(R.string.selection_select_all))
    }
    IconButton(
        onClick = {
            haptics.perform(JottiqHapticType.Confirm)
            onEvent(TrashEvent.RestoreSelectedClicked)
        },
        enabled = hasSelection,
    ) {
        AppIcon(AppIcons.Restore, contentDescription = stringResource(R.string.trash_restore_action))
    }
    IconButton(
        onClick = {
            haptics.perform(JottiqHapticType.Reject)
            onEvent(TrashEvent.DeleteForeverSelectedClicked)
        },
        enabled = hasSelection,
    ) {
        AppIcon(
            AppIcons.Delete,
            contentDescription = stringResource(R.string.selection_delete_forever_action),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun TrashList(
    items: List<TrashRowUi>,
    isLoading: Boolean,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onEvent: (TrashEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = JottiqSpacing.screenGutter,
            end = JottiqSpacing.screenGutter,
            top = contentPadding.calculateTopPadding() + JottiqSpacing.m,
            bottom = JottiqSpacing.listBottomInset,
        ),
    ) {
        item(key = "info-banner") {
            TrashInfoBanner(modifier = Modifier.padding(bottom = JottiqSpacing.sectionGap))
        }
        itemsIndexed(items, key = { _, row -> row.id }) { index, row ->
            TrashRow(
                row = row,
                index = index,
                count = items.size,
                selected = selectionMode && row.id in selectedIds,
                onClick = if (selectionMode) ({ onEvent(TrashEvent.SelectionToggled(row.id)) }) else null,
                onLongClick = if (selectionMode) null else ({ onEvent(TrashEvent.ItemLongPressed(row.id)) }),
                onRestoreClick = { onEvent(TrashEvent.RestoreClicked(row.id)) },
                showRestoreButton = !selectionMode,
                showSelectionIndicator = selectionMode,
                modifier = Modifier
                    .animateItem()
                    .padding(bottom = JottiqSpacing.groupGap),
            )
        }
        if (!isLoading && items.isEmpty()) {
            item(key = "empty-placeholder") {
                EmptyStateView(
                    icon = AppIcons.Delete,
                    title = stringResource(R.string.trash_empty_placeholder),
                )
            }
        }
    }
}

@Composable
private fun TrashInfoBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, JottiqShapes.medium)
            .padding(JottiqSpacing.l),
        horizontalArrangement = Arrangement.spacedBy(JottiqSpacing.m),
    ) {
        AppIcon(AppIcons.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.trash_info_banner),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrashRow(
    row: TrashRowUi,
    index: Int,
    count: Int,
    selected: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    onRestoreClick: () -> Unit,
    showRestoreButton: Boolean,
    showSelectionIndicator: Boolean,
    modifier: Modifier = Modifier,
) {
    val displayTitle = row.title.ifBlank { stringResource(R.string.untitled_note) }
    val folderLabel = row.folderName ?: stringResource(R.string.home_title)
    val daysLeftText = pluralStringResource(R.plurals.trash_days_left, row.daysLeft, row.daysLeft)
    val subtitle = stringResource(R.string.trash_row_in_folder, folderLabel) + " · " +
        stringResource(R.string.trash_row_deleted, row.deletedDateText) + " · " + daysLeftText

    GroupedListRow(
        index = index,
        count = count,
        headlineContent = {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            if (showSelectionIndicator) {
                SelectionIndicator(selected = selected)
            } else {
                AppIcon(
                    AppIcons.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = if (showRestoreButton) {
            {
                val haptics = rememberJottiqHaptics()
                IconButton(
                    onClick = {
                        haptics.perform(JottiqHapticType.Confirm)
                        onRestoreClick()
                    },
                ) {
                    AppIcon(
                        AppIcons.Restore,
                        contentDescription = stringResource(R.string.trash_restore_action),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            null
        },
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        modifier = modifier,
    )
}

@Composable
private fun EmptyTrashDialog(onEvent: (TrashEvent) -> Unit) {
    val haptics = rememberJottiqHaptics()
    AlertDialog(
        onDismissRequest = { onEvent(TrashEvent.EmptyTrashDialogDismissed) },
        title = { Text(stringResource(R.string.trash_empty_dialog_title)) },
        text = { Text(stringResource(R.string.trash_empty_dialog_body)) },
        confirmButton = {
            Button(
                onClick = {
                    haptics.perform(JottiqHapticType.Reject)
                    onEvent(TrashEvent.EmptyTrashConfirmed)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.trash_empty_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(TrashEvent.EmptyTrashDialogDismissed) }) {
                Text(stringResource(R.string.trash_empty_dialog_cancel))
            }
        },
    )
}

/** Confirms Trash's multi-select "Delete Forever" — modeled on [EmptyTrashDialog], since both hard-
 * delete a set of already-trashed notes and are irreversible (CLAUDE.md: never hard-delete outside
 * trash purge, and this counts as a purge of the selection). */
@Composable
private fun DeleteForeverDialog(onEvent: (TrashEvent) -> Unit) {
    val haptics = rememberJottiqHaptics()
    AlertDialog(
        onDismissRequest = { onEvent(TrashEvent.DeleteForeverDialogDismissed) },
        title = { Text(stringResource(R.string.selection_delete_forever_dialog_title)) },
        text = { Text(stringResource(R.string.selection_delete_forever_dialog_body)) },
        confirmButton = {
            Button(
                onClick = {
                    haptics.perform(JottiqHapticType.Reject)
                    onEvent(TrashEvent.DeleteForeverConfirmed)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.selection_delete_forever_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(TrashEvent.DeleteForeverDialogDismissed) }) {
                Text(stringResource(R.string.selection_delete_forever_dialog_cancel))
            }
        },
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun TrashScreenPreviewLight() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        TrashContent(uiState = previewUiState(), onEvent = {}, onBackClick = {})
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun TrashScreenPreviewDark() {
    JottiqTheme(darkTheme = true, dynamicColor = false) {
        TrashContent(uiState = previewUiState(), onEvent = {}, onBackClick = {})
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun TrashScreenPreviewEmpty() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        TrashContent(uiState = TrashUiState(isLoading = false), onEvent = {}, onBackClick = {})
    }
}

private fun previewUiState() = TrashUiState(
    isLoading = false,
    items = listOf(
        TrashRowUi(
            id = "1",
            title = "Old shopping list",
            folderName = null,
            deletedDateText = "12 Jun",
            daysLeft = 6,
        ),
        TrashRowUi(
            id = "2",
            title = "",
            folderName = "Work",
            deletedDateText = "28 Jun",
            daysLeft = 22,
        ),
    ),
)
