package com.anacardix.jottiq.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.rememberUpdatedState
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
import com.anacardix.jottiq.ui.common.NoteRowUi
import com.anacardix.jottiq.ui.common.NoteSectionLabel
import com.anacardix.jottiq.ui.common.NoteSectionUi
import com.anacardix.jottiq.ui.common.SelectionActions
import com.anacardix.jottiq.ui.common.folderRowGroup
import com.anacardix.jottiq.ui.common.noteRowGroup
import com.anacardix.jottiq.ui.common.resolve
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onFolderClick: (String) -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onLockedFolderClick: (String, String) -> Unit = { _, _ -> },
    onLockedNoteClick: (String, String) -> Unit = { _, _ -> },
    onTrashClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(HomeEvent.ScreenShown)
    }
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is HomeNavigationEvent.ToFolder -> onFolderClick(event.folderId)
                is HomeNavigationEvent.ToNote -> onNoteClick(event.noteId)
                is HomeNavigationEvent.ToLockedFolder -> onLockedFolderClick(event.folderId, event.name)
                is HomeNavigationEvent.ToLockedNote -> onLockedNoteClick(event.noteId, event.title)
                HomeNavigationEvent.ToTrash -> onTrashClick()
                HomeNavigationEvent.ToSettings -> onSettingsClick()
            }
        }
    }
    HomeContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val fabLift = rememberFabSnackbarLift(snackbarHostState)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val haptics = rememberJottiqHaptics()
    val currentHaptics by rememberUpdatedState(haptics)
    val userMessage = uiState.userMessage
    val resolvedUserMessage = userMessage?.resolve()
    val undoActionLabel = stringResource(R.string.undo_action)
    LaunchedEffect(userMessage?.id) {
        val message = userMessage ?: return@LaunchedEffect
        val text = resolvedUserMessage ?: return@LaunchedEffect
        val undo = message.undo
        try {
            val result = snackbarHostState.showSnackbar(
                message = text,
                actionLabel = if (undo != null) undoActionLabel else null,
                duration = SnackbarDuration.Short,
            )
            if (undo != null && result == SnackbarResult.ActionPerformed) {
                currentHaptics.perform(JottiqHapticType.Confirm)
                currentOnEvent(HomeEvent.UndoDeleteClicked(undo.noteIds, undo.folderIds))
            }
        } finally {
            // Runs even if navigating away cancels this coroutine mid-snackbar, so the
            // message is always marked consumed and never re-shown on the next recomposition.
            currentOnEvent(HomeEvent.UserMessageShown)
        }
    }

    BackHandler(enabled = uiState.selectionMode) { onEvent(HomeEvent.SelectionCancelled) }

    val scrollBehavior = rememberJottiqTopAppBarScrollBehavior()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (uiState.selectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectionCount,
                    onCancelClick = { onEvent(HomeEvent.SelectionCancelled) },
                    // Empty but non-null: keeps the bar at the same tall variant as HomeTopBar's
                    // title+subtitle, so the list doesn't jump when entering/exiting selection.
                    subtitle = { Text("") },
                    actions = {
                        SelectionActions(
                            hasSelectedNotes = uiState.selectedNoteIds.isNotEmpty(),
                            allSelectedFavorite = uiState.selectedNotesAllFavorite,
                            onSelectAllClick = { onEvent(HomeEvent.SelectAllClicked) },
                            onFavoriteToggleClick = { onEvent(HomeEvent.FavoriteSelectedClicked) },
                            onDeleteClick = { onEvent(HomeEvent.DeleteSelectedClicked) },
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            } else {
                HomeTopBar(
                    itemCount = uiState.itemCount,
                    sortOrder = uiState.sortOrder,
                    isSortMenuExpanded = uiState.isSortMenuExpanded,
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
            HomeList(
                favoriteNotes = uiState.favoriteNotes,
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
                    FabMenuScrim(onDismiss = { onEvent(HomeEvent.FabMenuToggled) })
                }
                JottiqFabMenu(
                    expanded = uiState.isFabMenuExpanded,
                    onToggle = { onEvent(HomeEvent.FabMenuToggled) },
                    toggleContentDescription = stringResource(R.string.home_toggle_fab_menu),
                    items = listOf(
                        JottiqFabMenuItem(
                            label = stringResource(R.string.home_new_folder),
                            icon = AppIcons.CreateNewFolder,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            onClick = { onEvent(HomeEvent.CreateFolderClicked) },
                        ),
                        JottiqFabMenuItem(
                            label = stringResource(R.string.home_new_note),
                            icon = AppIcons.EditNote,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            onClick = {
                                haptics.perform(JottiqHapticType.Confirm)
                                onEvent(HomeEvent.CreateNoteClicked)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    itemCount: Int,
    sortOrder: SortOrder,
    isSortMenuExpanded: Boolean,
    onEvent: (HomeEvent) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    JottiqTopAppBar(
        title = { Text(stringResource(R.string.home_title)) },
        subtitle = { Text(pluralStringResource(R.plurals.home_item_count, itemCount, itemCount)) },
        actions = {
            Box {
                IconButton(onClick = { onEvent(HomeEvent.SortMenuOpened) }) {
                    AppIcon(AppIcons.SwapVert, contentDescription = stringResource(R.string.home_sort_action))
                }
                SortDropdownMenu(
                    expanded = isSortMenuExpanded,
                    selected = sortOrder,
                    onDismiss = { onEvent(HomeEvent.SortMenuDismissed) },
                    onSelected = { onEvent(HomeEvent.SortOrderSelected(it)) },
                )
            }
            IconButton(onClick = { onEvent(HomeEvent.TrashClicked) }) {
                AppIcon(AppIcons.Delete, contentDescription = stringResource(R.string.home_trash_action))
            }
            IconButton(onClick = { onEvent(HomeEvent.SettingsClicked) }) {
                AppIcon(AppIcons.Settings, contentDescription = stringResource(R.string.home_settings_action))
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
private fun HomeList(
    favoriteNotes: List<NoteRowUi>,
    folders: List<FolderRowUi>,
    noteSections: List<NoteSectionUi>,
    isLoading: Boolean,
    isEmpty: Boolean,
    undoNonce: Int,
    selectionMode: Boolean,
    selectedNoteIds: Set<String>,
    selectedFolderIds: Set<String>,
    onEvent: (HomeEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    // No horizontal contentPadding here: it would shrink each item's measured width, so a fully
    // committed swipe-to-dismiss (which slides a row off by exactly its own width) would stop a
    // gutter's-width short of the physical screen edge. The gutter is applied per-item below
    // instead, so SwipeableGroupedRow's rows measure edge-to-edge and can clear the screen.
    // The "Notes" title + item count live in the JottiqTopAppBar's title/subtitle (see
    // HomeTopBar) rather than as a list header, so [contentPadding]'s top inset — which tracks the
    // app bar's current expanded/collapsed height — is the list's only top spacing.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = JottiqSpacing.listBottomInset,
        ),
    ) {
        if (favoriteNotes.isNotEmpty()) {
            item(key = "favorites-label") { FavoritesSectionLabel() }
            noteRowGroup(
                notes = favoriteNotes,
                showFavoriteIcon = false,
                onClick = { onEvent(HomeEvent.NoteClicked(it)) },
                onSwipeToDelete = { onEvent(HomeEvent.NoteSwipedToDelete(it)) },
                onSwipeToFavorite = { onEvent(HomeEvent.NoteSwipedToFavorite(it)) },
                keyPrefix = "favorite",
                resetSignal = undoNonce,
                selectionMode = selectionMode,
                selectedIds = selectedNoteIds,
                onLongPress = { onEvent(HomeEvent.ItemLongPressed(it, isFolder = false)) },
                onToggleSelection = { onEvent(HomeEvent.SelectionToggled(it, isFolder = false)) },
            )
            item(key = "favorites-gap") { Spacer(Modifier.height(JottiqSpacing.sectionGap)) }
        }
        if (folders.isNotEmpty()) {
            folderRowGroup(
                folders = folders,
                onClick = { onEvent(HomeEvent.FolderClicked(it)) },
                onSwipeToDelete = { onEvent(HomeEvent.FolderSwipedToDelete(it)) },
                resetSignal = undoNonce,
                selectionMode = selectionMode,
                selectedIds = selectedFolderIds,
                onLongPress = { onEvent(HomeEvent.ItemLongPressed(it, isFolder = true)) },
                onToggleSelection = { onEvent(HomeEvent.SelectionToggled(it, isFolder = true)) },
            )
            item(key = "folders-gap") { Spacer(Modifier.height(JottiqSpacing.sectionGap)) }
        }
        noteSections.forEachIndexed { index, section ->
            item(key = "section-$index") { NoteSectionLabel(section.group) }
            noteRowGroup(
                notes = section.notes,
                showFavoriteIcon = true,
                onClick = { onEvent(HomeEvent.NoteClicked(it)) },
                onSwipeToDelete = { onEvent(HomeEvent.NoteSwipedToDelete(it)) },
                onSwipeToFavorite = { onEvent(HomeEvent.NoteSwipedToFavorite(it)) },
                resetSignal = undoNonce,
                selectionMode = selectionMode,
                selectedIds = selectedNoteIds,
                onLongPress = { onEvent(HomeEvent.ItemLongPressed(it, isFolder = false)) },
                onToggleSelection = { onEvent(HomeEvent.SelectionToggled(it, isFolder = false)) },
            )
            if (index < noteSections.lastIndex) {
                item(key = "section-gap-$index") { Spacer(Modifier.height(JottiqSpacing.sectionGap)) }
            }
        }
        if (!isLoading && isEmpty && favoriteNotes.isEmpty()) {
            item(key = EMPTY_PLACEHOLDER_KEY) {
                EmptyHomePlaceholder(
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = tween(durationMillis = EMPTY_FADE_IN_MS, delayMillis = EMPTY_FADE_IN_DELAY_MS),
                            fadeOutSpec = tween(durationMillis = EMPTY_FADE_OUT_MS),
                        )
                        .padding(horizontal = JottiqSpacing.screenGutter),
                )
            }
        }
    }
}

private const val EMPTY_PLACEHOLDER_KEY = "empty-placeholder"

// Matches the generous header-to-icon gap in `design/18. No notes.png` — noticeably more than the
// EmptyStateView default, which is tuned for tighter contexts like Trash.
private val EMPTY_PLACEHOLDER_TOP_SPACING = 72.dp

// Delayed so the placeholder eases in only after a swiped row's own exit animation clears,
// instead of popping in over it.
private const val EMPTY_FADE_IN_DELAY_MS = 140
private const val EMPTY_FADE_IN_MS = 260
private const val EMPTY_FADE_OUT_MS = 120

@Composable
private fun FavoritesSectionLabel() {
    Row(
        modifier = Modifier
            .padding(horizontal = JottiqSpacing.screenGutter)
            .padding(bottom = JottiqSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JottiqSpacing.xs),
    ) {
        AppIcon(
            AppIcons.Star,
            contentDescription = null,
            filled = true,
            tint = MaterialTheme.colorScheme.primary,
            sizeSp = 18,
        )
        Text(
            text = stringResource(R.string.home_favorites),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyHomePlaceholder(modifier: Modifier = Modifier) {
    EmptyStateView(
        icon = AppIcons.EditNote,
        title = stringResource(R.string.home_empty_title),
        message = stringResource(R.string.home_empty_subtitle),
        modifier = modifier,
        topSpacing = EMPTY_PLACEHOLDER_TOP_SPACING,
    )
}

@Composable
private fun CreateFolderDialog(name: String, onEvent: (HomeEvent) -> Unit) {
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
            onEvent(HomeEvent.CreateFolderConfirmed)
        },
        onDismiss = { onEvent(HomeEvent.CreateFolderDialogDismissed) },
    ) {
        JottiqDialogTextField(
            value = name,
            onValueChange = { onEvent(HomeEvent.CreateFolderNameChanged(it)) },
            placeholder = nameLabel,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun HomeScreenPreviewLight() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        HomeContent(uiState = previewUiState(), onEvent = {})
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun HomeScreenPreviewDark() {
    JottiqTheme(darkTheme = true, dynamicColor = false) {
        HomeContent(uiState = previewUiState(), onEvent = {})
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun HomeScreenPreviewEmpty() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        HomeContent(uiState = HomeUiState(isLoading = false), onEvent = {})
    }
}

private fun previewUiState(): HomeUiState {
    val groceries = NoteRowUi(
        "1",
        "Groceries",
        isFavorite = true,
        isLocked = false,
        dateLabel = RelativeDateLabel.Time("14:02"),
    )
    val kyotoItinerary = NoteRowUi(
        "2",
        "Kyoto itinerary",
        isFavorite = true,
        isLocked = false,
        dateLabel = RelativeDateLabel.Time("09:15"),
    )
    val booksToRead = NoteRowUi(
        "3",
        "Books to read",
        isFavorite = false,
        isLocked = false,
        dateLabel = RelativeDateLabel.Date("28 Jun"),
    )
    val giftIdeas = NoteRowUi(
        "4",
        "Gift ideas",
        isFavorite = false,
        isLocked = true,
        dateLabel = RelativeDateLabel.Date("21 Jun"),
    )
    return HomeUiState(
        isLoading = false,
        itemCount = 7,
        favoriteNotes = listOf(groceries, kyotoItinerary),
        folders = listOf(
            FolderRowUi("journal", "Journal", noteCount = 2, isLocked = true),
            FolderRowUi("personal", "Personal", noteCount = 3, isLocked = false),
            FolderRowUi("sketches", "Sketches", noteCount = 0, isLocked = false),
            FolderRowUi("work", "Work", noteCount = 2, isLocked = false),
        ),
        noteSections = listOf(
            NoteSectionUi(NoteDateGroup.Today, listOf(groceries)),
            NoteSectionUi(NoteDateGroup.Month("June"), listOf(booksToRead, giftIdeas)),
        ),
    )
}
