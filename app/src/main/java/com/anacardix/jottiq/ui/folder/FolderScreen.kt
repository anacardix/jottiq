package com.anacardix.jottiq.ui.folder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anacardix.jottiq.R
import com.anacardix.jottiq.designsystem.JottiqHapticType
import com.anacardix.jottiq.designsystem.JottiqSpacing
import com.anacardix.jottiq.designsystem.JottiqTheme
import com.anacardix.jottiq.designsystem.component.AnimatedSnackbarHost
import com.anacardix.jottiq.designsystem.component.EmptyStateView
import com.anacardix.jottiq.designsystem.component.FabMenuScrim
import com.anacardix.jottiq.designsystem.component.JottiqDialogTextField
import com.anacardix.jottiq.designsystem.component.JottiqFabMenu
import com.anacardix.jottiq.designsystem.component.JottiqFabMenuItem
import com.anacardix.jottiq.designsystem.component.JottiqInputDialog
import com.anacardix.jottiq.designsystem.component.JottiqLoadingIndicator
import com.anacardix.jottiq.designsystem.component.JottiqTopAppBar
import com.anacardix.jottiq.designsystem.component.SelectionTopBar
import com.anacardix.jottiq.designsystem.component.rememberFabSnackbarLift
import com.anacardix.jottiq.designsystem.component.rememberJottiqTopAppBarScrollBehavior
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIconGlyph
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.anacardix.jottiq.designsystem.rememberJottiqHaptics
import com.anacardix.jottiq.domain.SortOrder
import com.anacardix.jottiq.domain.usecase.NoteDateGroup
import com.anacardix.jottiq.domain.usecase.RelativeDateLabel
import com.anacardix.jottiq.ui.common.FolderRowUi
import com.anacardix.jottiq.ui.common.MoveToFolderSheet
import com.anacardix.jottiq.ui.common.NoteRowUi
import com.anacardix.jottiq.ui.common.NoteSectionLabel
import com.anacardix.jottiq.ui.common.NoteSectionUi
import com.anacardix.jottiq.ui.common.SelectionActions
import com.anacardix.jottiq.ui.common.folderRowGroup
import com.anacardix.jottiq.ui.common.noteRowGroup
import com.anacardix.jottiq.ui.common.resolve
import kotlinx.coroutines.flow.collectLatest

@Composable
fun FolderScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onFolderClick: (String) -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onLockedFolderClick: (String, String) -> Unit = { _, _ -> },
    onLockedNoteClick: (String, String) -> Unit = { _, _ -> },
    viewModel: FolderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(FolderEvent.ScreenShown)
    }
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is FolderNavigationEvent.ToFolder -> onFolderClick(event.folderId)
                is FolderNavigationEvent.ToNote -> onNoteClick(event.noteId)
                is FolderNavigationEvent.ToLockedFolder -> onLockedFolderClick(event.folderId, event.name)
                is FolderNavigationEvent.ToLockedNote -> onLockedNoteClick(event.noteId, event.title)
            }
        }
    }
    FolderContent(uiState = uiState, onEvent = viewModel::onEvent, onBackClick = onBackClick, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderContent(
    uiState: FolderUiState,
    onEvent: (FolderEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val fabLift = rememberFabSnackbarLift(snackbarHostState)
    val haptics = rememberJottiqHaptics()
    val userMessage = uiState.userMessage
    val resolvedUserMessage = userMessage?.resolve()
    val undoActionLabel = stringResource(R.string.undo_action)
    LaunchedEffect(userMessage?.id) {
        if (resolvedUserMessage != null) {
            val undo = userMessage?.undo
            try {
                val result = snackbarHostState.showSnackbar(
                    message = resolvedUserMessage,
                    actionLabel = if (undo != null) undoActionLabel else null,
                    duration = SnackbarDuration.Short,
                )
                if (undo != null && result == SnackbarResult.ActionPerformed) {
                    haptics.perform(JottiqHapticType.Confirm)
                    onEvent(FolderEvent.UndoDeleteClicked(undo.noteIds, undo.folderIds))
                }
            } finally {
                // Runs even if navigating away cancels this coroutine mid-snackbar, so the
                // message is always marked consumed and never re-shown on the next recomposition.
                onEvent(FolderEvent.UserMessageShown)
            }
        }
    }

    BackHandler(enabled = uiState.selectionMode) { onEvent(FolderEvent.SelectionCancelled) }

    val scrollBehavior = rememberJottiqTopAppBarScrollBehavior()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (uiState.selectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectionCount,
                    onCancelClick = { onEvent(FolderEvent.SelectionCancelled) },
                    // A single space, not an empty string: keeps the bar at the same tall variant
                    // as FolderTopBar's title+subtitle. `Text("")` measures a shorter line than any
                    // non-empty string (including whitespace-only) in this material3 version, so an
                    // actually-empty subtitle would shrink the bar and jump the list content up.
                    subtitle = " ",
                    actions = {
                        SelectionActions(
                            hasSelectedNotes = uiState.selectedNoteIds.isNotEmpty(),
                            hasSelectedFolders = uiState.selectedFolderIds.isNotEmpty(),
                            allSelectedFavorite = uiState.selectedNotesAllFavorite,
                            onSelectAllClick = { onEvent(FolderEvent.SelectAllClicked) },
                            onFavoriteToggleClick = { onEvent(FolderEvent.FavoriteSelectedClicked) },
                            onMoveClick = { onEvent(FolderEvent.MoveSelectedClicked) },
                            onDeleteClick = { onEvent(FolderEvent.DeleteSelectedClicked) },
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            } else {
                FolderTopBar(
                    folderName = uiState.folderName,
                    itemCount = uiState.itemCount,
                    isLocked = uiState.isLocked,
                    sortOrder = uiState.sortOrder,
                    isSortMenuExpanded = uiState.isSortMenuExpanded,
                    onBackClick = onBackClick,
                    onEvent = onEvent,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        snackbarHost = {
            AnimatedSnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.onSizeChanged(fabLift.onSnackbarSized),
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            FolderList(
                folders = uiState.folders,
                noteSections = uiState.noteSections,
                isLoading = uiState.isLoading,
                isEmpty = uiState.isEmpty,
                undoNonce = uiState.undoNonce,
                selectionMode = uiState.selectionMode,
                selectedNoteIds = uiState.selectedNoteIds,
                selectedFolderIds = uiState.selectedFolderIds,
                onEvent = onEvent,
                contentPadding = innerPadding,
            )
            if (uiState.isLoading) {
                JottiqLoadingIndicator()
            }
            if (!uiState.selectionMode) {
                if (uiState.isFabMenuExpanded) {
                    FabMenuScrim(onDismiss = { onEvent(FolderEvent.FabMenuToggled) })
                }
                JottiqFabMenu(
                    expanded = uiState.isFabMenuExpanded,
                    onToggle = { onEvent(FolderEvent.FabMenuToggled) },
                    toggleContentDescription = stringResource(R.string.home_toggle_fab_menu),
                    items = listOf(
                        JottiqFabMenuItem(
                            label = stringResource(R.string.home_new_folder),
                            icon = AppIcons.CreateNewFolder,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            onClick = { onEvent(FolderEvent.CreateFolderClicked) },
                        ),
                        JottiqFabMenuItem(
                            label = stringResource(R.string.home_new_note),
                            icon = AppIcons.EditNote,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            onClick = {
                                haptics.perform(JottiqHapticType.Confirm)
                                onEvent(FolderEvent.CreateNoteClicked)
                            },
                        ),
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset { IntOffset(x = 0, y = -fabLift.offset.roundToPx()) },
                )
            }
        }
    }

    if (uiState.isCreateFolderDialogVisible) {
        CreateFolderDialog(name = uiState.createFolderName, onEvent = onEvent)
    }

    if (uiState.isMoveSheetVisible) {
        MoveToFolderSheet(
            folders = uiState.moveFolders,
            selectedFolderId = uiState.selectedMoveFolderId,
            onFolderSelected = { onEvent(FolderEvent.MoveSelectionFolderSelected(it)) },
            onDismiss = { onEvent(FolderEvent.MoveSelectionSheetDismissed) },
            onConfirm = { onEvent(FolderEvent.MoveSelectionConfirmed) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderTopBar(
    folderName: String,
    itemCount: Int,
    isLocked: Boolean,
    sortOrder: SortOrder,
    isSortMenuExpanded: Boolean,
    onBackClick: () -> Unit,
    onEvent: (FolderEvent) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val haptics = rememberJottiqHaptics()
    JottiqTopAppBar(
        title = folderName,
        subtitle = pluralStringResource(R.plurals.home_item_count, itemCount, itemCount),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                AppIcon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.folder_back_action))
            }
        },
        actions = {
            IconButton(
                onClick = {
                    haptics.perform(if (isLocked) JottiqHapticType.ToggleOff else JottiqHapticType.ToggleOn)
                    onEvent(FolderEvent.LockToggleClicked)
                },
            ) {
                AppIcon(
                    if (isLocked) AppIcons.Lock else AppIcons.LockOpen,
                    contentDescription = stringResource(
                        if (isLocked) R.string.folder_unlock_action else R.string.folder_lock_action,
                    ),
                    filled = isLocked,
                )
            }
            Box {
                IconButton(onClick = { onEvent(FolderEvent.SortMenuOpened) }) {
                    AppIcon(AppIcons.SwapVert, contentDescription = stringResource(R.string.home_sort_action))
                }
                SortDropdownMenu(
                    expanded = isSortMenuExpanded,
                    selected = sortOrder,
                    onDismiss = { onEvent(FolderEvent.SortMenuDismissed) },
                    onSelected = { onEvent(FolderEvent.SortOrderSelected(it)) },
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    selected: SortOrder,
    onDismiss: () -> Unit,
    onSelected: (SortOrder) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.home_sort_by),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = JottiqSpacing.l, vertical = JottiqSpacing.s),
        )
        SortMenuOption(
            glyph = AppIcons.Restore,
            label = stringResource(R.string.home_sort_date_edited),
            isSelected = selected == SortOrder.DateEdited,
            onClick = { onSelected(SortOrder.DateEdited) },
        )
        SortMenuOption(
            glyph = AppIcons.CalendarToday,
            label = stringResource(R.string.home_sort_date_created),
            isSelected = selected == SortOrder.DateCreated,
            onClick = { onSelected(SortOrder.DateCreated) },
        )
        SortMenuOption(
            glyph = AppIcons.SortByAlpha,
            label = stringResource(R.string.home_sort_title_az),
            isSelected = selected == SortOrder.TitleAsc,
            onClick = { onSelected(SortOrder.TitleAsc) },
        )
    }
}

@Composable
private fun SortMenuOption(
    glyph: AppIconGlyph,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberJottiqHaptics()
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { AppIcon(glyph, contentDescription = null) },
        trailingIcon = {
            if (isSelected) {
                AppIcon(
                    AppIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        onClick = {
            haptics.perform(JottiqHapticType.Confirm)
            onClick()
        },
    )
}

@Composable
private fun FolderList(
    folders: List<FolderRowUi>,
    noteSections: List<NoteSectionUi>,
    isLoading: Boolean,
    isEmpty: Boolean,
    undoNonce: Int,
    selectionMode: Boolean,
    selectedNoteIds: Set<String>,
    selectedFolderIds: Set<String>,
    onEvent: (FolderEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    // No horizontal contentPadding here: it would shrink each item's measured width, so a fully
    // committed swipe-to-dismiss (which slides a row off by exactly its own width) would stop a
    // gutter's-width short of the physical screen edge. The gutter is applied per-item below
    // instead, so SwipeableGroupedRow's rows measure edge-to-edge and can clear the screen.
    // The folder name + item count live in the JottiqTopAppBar's title/subtitle (see
    // FolderTopBar) rather than as a list header, so [contentPadding]'s top inset — which tracks
    // the app bar's current expanded/collapsed height — is the list's only top spacing.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = JottiqSpacing.listBottomInset,
        ),
    ) {
        if (folders.isNotEmpty()) {
            folderRowGroup(
                folders = folders,
                onClick = { onEvent(FolderEvent.FolderClicked(it)) },
                onSwipeToDelete = { onEvent(FolderEvent.FolderSwipedToDelete(it)) },
                resetSignal = undoNonce,
                selectionMode = selectionMode,
                selectedIds = selectedFolderIds,
                onLongPress = { onEvent(FolderEvent.ItemLongPressed(it, isFolder = true)) },
                onToggleSelection = { onEvent(FolderEvent.SelectionToggled(it, isFolder = true)) },
            )
            item(key = "folders-gap") { Spacer(Modifier.height(JottiqSpacing.sectionGap)) }
        }
        noteSections.forEachIndexed { index, section ->
            item(key = "section-$index") { NoteSectionLabel(section.group) }
            noteRowGroup(
                notes = section.notes,
                showFavoriteIcon = true,
                onClick = { onEvent(FolderEvent.NoteClicked(it)) },
                onSwipeToDelete = { onEvent(FolderEvent.NoteSwipedToDelete(it)) },
                onSwipeToFavorite = { onEvent(FolderEvent.NoteSwipedToFavorite(it)) },
                resetSignal = undoNonce,
                selectionMode = selectionMode,
                selectedIds = selectedNoteIds,
                onLongPress = { onEvent(FolderEvent.ItemLongPressed(it, isFolder = false)) },
                onToggleSelection = { onEvent(FolderEvent.SelectionToggled(it, isFolder = false)) },
            )
            if (index < noteSections.lastIndex) {
                item(key = "section-gap-$index") { Spacer(Modifier.height(JottiqSpacing.sectionGap)) }
            }
        }
        if (!isLoading && isEmpty) {
            item(key = "empty-placeholder") {
                EmptyFolderPlaceholder(modifier = Modifier.padding(horizontal = JottiqSpacing.screenGutter))
            }
        }
    }
}

// Matches the generous header-to-icon gap in `design/19. No notes in folder.png` — noticeably more
// than the EmptyStateView default, which is tuned for tighter contexts like Trash.
private val EMPTY_PLACEHOLDER_TOP_SPACING = 72.dp

@Composable
private fun EmptyFolderPlaceholder(modifier: Modifier = Modifier) {
    EmptyStateView(
        icon = AppIcons.FolderOpen,
        title = stringResource(R.string.folder_empty_title),
        message = stringResource(R.string.folder_empty_subtitle),
        modifier = modifier,
        topSpacing = EMPTY_PLACEHOLDER_TOP_SPACING,
    )
}

@Composable
private fun CreateFolderDialog(name: String, onEvent: (FolderEvent) -> Unit) {
    val haptics = rememberJottiqHaptics()
    // Resolved here, not inside the content lambda below: AlertDialog's content slot is composed
    // in its own window, which re-provides LocalContext from the real Activity context, shadowing
    // LocalizedContent's in-app language override. stringResource() must run outside that boundary.
    val nameLabel = stringResource(R.string.home_create_folder_name_label)
    JottiqInputDialog(
        title = stringResource(R.string.home_create_folder_title),
        confirmLabel = stringResource(R.string.home_create_folder_confirm),
        dismissLabel = stringResource(R.string.home_create_folder_cancel),
        confirmEnabled = name.isNotBlank(),
        onConfirm = {
            haptics.perform(JottiqHapticType.Confirm)
            onEvent(FolderEvent.CreateFolderConfirmed)
        },
        onDismiss = { onEvent(FolderEvent.CreateFolderDialogDismissed) },
    ) {
        JottiqDialogTextField(
            value = name,
            onValueChange = { onEvent(FolderEvent.CreateFolderNameChanged(it)) },
            placeholder = nameLabel,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun FolderScreenPreviewLight() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        FolderContent(uiState = previewUiState(), onEvent = {}, onBackClick = {})
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun FolderScreenPreviewDark() {
    JottiqTheme(darkTheme = true, dynamicColor = false) {
        FolderContent(uiState = previewUiState(), onEvent = {}, onBackClick = {})
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun FolderScreenPreviewEmpty() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        FolderContent(
            uiState = FolderUiState(isLoading = false, folderName = "Sketches"),
            onEvent = {},
            onBackClick = {},
        )
    }
}

private fun previewUiState() = FolderUiState(
    isLoading = false,
    folderName = "Personal",
    itemCount = 2,
    folders = listOf(FolderRowUi("travel", "Travel", noteCount = 2, isLocked = false)),
    noteSections = listOf(
        NoteSectionUi(
            NoteDateGroup.Month("June"),
            listOf(
                NoteRowUi(
                    id = "1",
                    title = "Apartment ideas",
                    isFavorite = false,
                    isLocked = false,
                    dateLabel = RelativeDateLabel.Date("15 Jun"),
                ),
            ),
        ),
    ),
)
