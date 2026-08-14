package com.anacardix.jottiq.ui.noteeditor

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anacardix.jottiq.R
import com.anacardix.jottiq.designsystem.JottiqHapticType
import com.anacardix.jottiq.designsystem.JottiqNoteBodyTextStyle
import com.anacardix.jottiq.designsystem.JottiqSpacing
import com.anacardix.jottiq.designsystem.JottiqTheme
import com.anacardix.jottiq.designsystem.component.JottiqDialogTextField
import com.anacardix.jottiq.designsystem.component.JottiqFloatingToolbar
import com.anacardix.jottiq.designsystem.component.JottiqHeadingToggleButton
import com.anacardix.jottiq.designsystem.component.JottiqInputDialog
import com.anacardix.jottiq.designsystem.component.JottiqLoadingIndicator
import com.anacardix.jottiq.designsystem.icon.AppIcon
import com.anacardix.jottiq.designsystem.icon.AppIconGlyph
import com.anacardix.jottiq.designsystem.icon.AppIcons
import com.anacardix.jottiq.designsystem.rememberJottiqHaptics
import com.anacardix.jottiq.domain.HeadingLevel
import com.anacardix.jottiq.domain.NoteTextColor
import com.anacardix.jottiq.ui.common.resolve
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Composable
fun NoteEditorScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(NoteEditorEvent.ScreenShown)
    }
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                NoteEditorNavigationEvent.Back -> onBackClick()
            }
        }
    }
    NoteEditorContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteEditorContent(
    uiState: NoteEditorUiState,
    onEvent: (NoteEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only intercept back while editing (commit-to-view-mode / discard-empty-note, see
    // NoteEditorViewModel.onBackClicked). In view mode there's nothing left to do on the way
    // out, so leaving the handler disabled lets the system's predictive-back gesture drive
    // JottiqNavHost's pop transition directly — the same animation every other screen gets.
    BackHandler(enabled = uiState.isEditing) { onEvent(NoteEditorEvent.BackClicked) }
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
                onEvent(NoteEditorEvent.UserMessageShown)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { NoteEditorTopBar(uiState = uiState, onEvent = onEvent) },
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
            NoteEditorBody(uiState = uiState, onEvent = onEvent, contentPadding = innerPadding)
            if (uiState.isLoading) {
                JottiqLoadingIndicator()
            }
            if (uiState.isEditing) {
                EditorToolbar(
                    uiState = uiState,
                    onEvent = onEvent,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                        .fillMaxWidth()
                        .padding(horizontal = JottiqSpacing.screenGutter)
                        .padding(bottom = JottiqSpacing.l),
                )
            }
        }
    }

    if (uiState.isLinkDialogVisible) {
        InsertLinkDialog(displayText = uiState.linkDisplayText, url = uiState.linkUrl, onEvent = onEvent)
    }

    if (uiState.isMoveSheetVisible) {
        MoveToFolderSheet(
            folders = uiState.moveFolders,
            selectedFolderId = uiState.selectedMoveFolderId,
            onEvent = onEvent,
        )
    }

    if (uiState.isDeleteDialogVisible) {
        DeleteConfirmationDialog(title = uiState.title, onEvent = onEvent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorTopBar(uiState: NoteEditorUiState, onEvent: (NoteEditorEvent) -> Unit) {
    val haptics = rememberJottiqHaptics()
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = { onEvent(NoteEditorEvent.BackClicked) }) {
                AppIcon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.note_editor_back_action))
            }
        },
        actions = {
            // Kept visible in both read and edit mode (see [EditorToolbar], the formatting dock,
            // which stays docked at the bottom) so leaving a note only takes a keyboard-dismiss
            // plus one Back: there's no separate "commit to read mode" step in between anymore.
            // Non-focusable while editing — same reason every dock control uses it (see
            // ToolbarToggle below): grabbing focus here would end the text field's IME session,
            // collapsing the selection and risking dropped formatting spans.
            val actionModifier = if (uiState.isEditing) Modifier.focusProperties { canFocus = false } else Modifier
            IconButton(
                onClick = {
                    haptics.perform(if (uiState.isFavorite) JottiqHapticType.ToggleOff else JottiqHapticType.ToggleOn)
                    onEvent(NoteEditorEvent.FavoriteClicked)
                },
                modifier = actionModifier,
            ) {
                AppIcon(
                    AppIcons.Star,
                    contentDescription = stringResource(R.string.note_editor_favorite_action),
                    filled = uiState.isFavorite,
                    tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
            IconButton(
                onClick = {
                    haptics.perform(if (uiState.isLocked) JottiqHapticType.ToggleOff else JottiqHapticType.ToggleOn)
                    onEvent(NoteEditorEvent.LockClicked)
                },
                modifier = actionModifier,
            ) {
                AppIcon(
                    if (uiState.isLocked) AppIcons.Lock else AppIcons.LockOpen,
                    contentDescription = stringResource(R.string.note_editor_lock_action),
                    filled = uiState.isLocked,
                )
            }
            IconButton(onClick = { onEvent(NoteEditorEvent.MoveClicked) }, modifier = actionModifier) {
                AppIcon(AppIcons.DriveFileMove, contentDescription = stringResource(R.string.note_editor_move_action))
            }
            IconButton(onClick = { onEvent(NoteEditorEvent.DeleteClicked) }, modifier = actionModifier) {
                AppIcon(AppIcons.Delete, contentDescription = stringResource(R.string.note_editor_delete_action))
            }
        },
    )
}

// A plain Column (not a LazyColumn): notes are modest-sized documents, and lazy recycling of live
// text fields costs more than it saves here — scrolled-away editors lose their composition (and
// with it IME sessions and focus bookkeeping), which caused visible jank while typing/scrolling.
@Composable
private fun NoteEditorBody(
    uiState: NoteEditorUiState,
    onEvent: (NoteEditorEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val titleFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(uiState.pendingFocus) {
        when (val target = uiState.pendingFocus) {
            null -> return@LaunchedEffect
            EditorFocusTarget.Title -> titleFocusRequester.requestFocus()
            is EditorFocusTarget.Segment -> focusRequesters[target.id]?.requestFocus()
        }
        // Let the focus change — and any bring-into-view scroll BasicTextField triggers for it —
        // land on its own frame before the keyboard starts sliding in. Showing both at once (most
        // visible appending a new bulleted line below an existing list while the keyboard was
        // closed) let the field's auto-scroll re-chase the animating IME inset on every frame of
        // that animation, visibly juddering the whole note above it.
        withFrameNanos {}
        // requestFocus() is a no-op — and fires no onFocusChanged — when the target already holds
        // focus (e.g. tapping the empty area below a blank note's single, already-focused segment
        // after Back dismissed the IME). pendingFocus is always an explicit "focus here to edit"
        // signal, so show the keyboard directly rather than relying on a focus transition that may
        // not happen.
        keyboard?.show()
        onEvent(NoteEditorEvent.FocusRequestConsumed)
    }
    val bodyTapInteraction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(NOTE_BODY_TAG)
            .verticalScroll(rememberScrollState())
            // Tapping the empty area past the end of the content drops the caret into the
            // last segment — in read mode this also enters edit mode; in edit mode it moves
            // focus there from wherever it was (e.g. the title). Child fields/rows (the rich
            // text editors, the title field, read-mode text) consume their own taps first, so
            // this only fires on genuinely empty space.
            .clickable(interactionSource = bodyTapInteraction, indication = null) {
                onEvent(NoteEditorEvent.EditModeRequested())
            }
            .imePadding()
            .padding(horizontal = JottiqSpacing.screenGutter)
            .padding(top = contentPadding.calculateTopPadding(), bottom = JottiqSpacing.listBottomInset),
    ) {
        NoteTitleAndSubtitle(uiState = uiState, onEvent = onEvent, titleFocusRequester = titleFocusRequester)
        uiState.segments.forEachIndexed { index, segment ->
            key(segment.id) {
                val focusRequester = remember { FocusRequester().also { focusRequesters[segment.id] = it } }
                SegmentRow(
                    segment = segment,
                    isEditing = uiState.isEditing,
                    isFirstSegment = index == 0,
                    focusRequester = focusRequester,
                    onEvent = onEvent,
                    modifier = Modifier.padding(bottom = JottiqSpacing.xs),
                )
            }
        }
        if (uiState.isEditing) {
            // Keeps the last lines reachable above the floating toolbar.
            Spacer(modifier = Modifier.height(TOOLBAR_CLEARANCE))
        }
    }
}

@Composable
private fun NoteTitleAndSubtitle(
    uiState: NoteEditorUiState,
    onEvent: (NoteEditorEvent) -> Unit,
    titleFocusRequester: FocusRequester,
) {
    // A singleLine BasicTextField measures shorter than a Text with the same style (it collapses
    // toward glyph height and drops the style's line leading), which used to pull the subtitle and
    // body up the moment editing started. Normalizing font padding/line-height trim and reserving
    // one full title line in both branches keeps read and edit mode pixel-identical.
    val titleStyle = MaterialTheme.typography.headlineMedium.merge(
        TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        ),
    )
    val titleMinHeight = with(LocalDensity.current) {
        MaterialTheme.typography.headlineMedium.lineHeight.toDp()
    }
    val keyboard = LocalSoftwareKeyboardController.current
    Column(modifier = Modifier.padding(top = JottiqSpacing.l, bottom = JottiqSpacing.s)) {
        if (uiState.isEditing) {
            Box(modifier = Modifier.heightIn(min = titleMinHeight)) {
                if (uiState.title.isEmpty()) {
                    // Shown behind the (still-empty) field, not written into it — so typing
                    // immediately replaces it without deleting anything first, and the real title
                    // (and the empty-note-discarded-on-back check) stays blank until the user
                    // actually types something.
                    Text(
                        text = stringResource(R.string.untitled_note),
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = uiState.title,
                    onValueChange = { onEvent(NoteEditorEvent.TitleChanged(it)) },
                    textStyle = titleStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { onEvent(NoteEditorEvent.TitleNextPressed) }),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .reshowKeyboardOnTap(keyboard)
                        .testTag(NOTE_TITLE_FIELD_TAG),
                )
            }
        } else {
            Text(
                text = uiState.title.ifBlank { stringResource(R.string.untitled_note) },
                style = titleStyle,
                modifier = Modifier
                    .heightIn(min = titleMinHeight)
                    .clickable { onEvent(NoteEditorEvent.EditModeRequested()) },
            )
        }
        val dateTextRes = if (uiState.wasEdited) R.string.note_editor_edited else R.string.note_editor_created
        val dateText = stringResource(dateTextRes, uiState.dateLabel)
        val subtitle = if (uiState.isFavorite) {
            dateText + " · " + stringResource(R.string.note_editor_favorite_suffix)
        } else {
            dateText
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = JottiqSpacing.xs),
        )
    }
}

@Composable
private fun SegmentRow(
    segment: EditorSegment,
    isEditing: Boolean,
    isFirstSegment: Boolean,
    focusRequester: FocusRequester,
    onEvent: (NoteEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (segment) {
        is EditorSegment.Rich -> RichSegmentRow(
            segment = segment,
            isEditing = isEditing,
            isFirstSegment = isFirstSegment,
            focusRequester = focusRequester,
            onEvent = onEvent,
            modifier = modifier,
        )
    }
}

// The IME is only ever shown implicitly, as a side effect of a field gaining focus (see
// pendingFocus/FocusRequester below). If the user dismisses the keyboard with the back gesture,
// the field keeps Compose focus (the caret stays visible), so a later tap is not a focus change
// and nothing reopens the keyboard. Re-request it on every pointer-down while editing instead.
private fun Modifier.reshowKeyboardOnTap(keyboard: SoftwareKeyboardController?): Modifier =
    if (keyboard == null) {
        this
    } else {
        pointerInput(keyboard) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false) // don't consume — the editor still gets the tap
                keyboard.show()
            }
        }
    }

@Composable
private fun RichSegmentRow(
    segment: EditorSegment.Rich,
    isEditing: Boolean,
    isFirstSegment: Boolean,
    focusRequester: FocusRequester,
    onEvent: (NoteEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only the very first segment gets the "Start typing…" hint — a blank paragraph created lower
    // in the note shouldn't repeat it line after line. Shown in both modes (an empty saved note is
    // still empty on reopen, before any tap enters edit mode).
    val showPlaceholder = isFirstSegment && segment.state.annotatedString.text.isEmpty()
    if (!isEditing) {
        Box(modifier = modifier.fillMaxWidth()) {
            if (showPlaceholder) {
                Text(
                    text = stringResource(R.string.note_editor_body_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ReadRichSegment(
                state = segment.state,
                onTap = { onEvent(NoteEditorEvent.EditModeRequested(segment.id)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }
    // Keyed on the live state (not the segment id): structural edits swap in a rebuilt state under
    // the same id, and the observer must follow the new instance.
    LaunchedEffect(segment.state) {
        snapshotFlow { segment.state.annotatedString }
            .drop(1)
            .collect { onEvent(NoteEditorEvent.SegmentContentChanged(segment.id)) }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        if (showPlaceholder) {
            Text(
                text = stringResource(R.string.note_editor_body_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val keyboard = LocalSoftwareKeyboardController.current
        BasicRichTextEditor(
            state = segment.state,
            textStyle = JottiqNoteBodyTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .reshowKeyboardOnTap(keyboard)
                .onFocusChanged { state ->
                    if (state.isFocused) onEvent(NoteEditorEvent.SegmentFocusChanged(segment.id))
                },
        )
    }
}

@Composable
private fun EditorToolbar(
    uiState: NoteEditorUiState,
    onEvent: (NoteEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Segments/focusedSegmentId rarely change; remembered so a cursor move or style change (which
    // recomposes this toolbar via the currentSpanStyle/currentHeadingStyle reads below) doesn't
    // re-scan the segment list every time.
    val focusedSegment = remember(uiState.segments, uiState.focusedSegmentId) {
        uiState.segments.firstOrNull { it.id == uiState.focusedSegmentId }
    }
    val focusedState = focusedSegment?.state
    val currentStyle = focusedState?.currentSpanStyle
    val boldActive = currentStyle?.fontWeight == FontWeight.Bold
    val italicActive = currentStyle?.fontStyle == FontStyle.Italic
    val underlineActive = currentStyle?.textDecoration == TextDecoration.Underline
    val activeColor = currentStyle?.color?.takeIf { it != Color.Unspecified }
    val focusedHeading = (focusedSegment as? EditorSegment.Rich)?.state?.currentHeadingStyle?.toHeadingLevel()
    val bulletActive = (focusedSegment as? EditorSegment.Rich)?.state?.isUnorderedList == true
    val numberedListActive = (focusedSegment as? EditorSegment.Rich)?.state?.isOrderedList == true

    JottiqFloatingToolbar(modifier = modifier) {
        Row(
            // The toolbar packs 9 controls, which can be wider than a narrow phone's screen —
            // scrolling this content row internally, instead of letting it overflow, keeps every
            // control reachable.
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarToggle(
                glyph = AppIcons.FormatBold,
                contentDescription = stringResource(R.string.note_editor_bold_action),
                active = boldActive,
                onClick = { onEvent(NoteEditorEvent.BoldClicked) },
            )
            ToolbarToggle(
                glyph = AppIcons.FormatItalic,
                contentDescription = stringResource(R.string.note_editor_italic_action),
                active = italicActive,
                onClick = { onEvent(NoteEditorEvent.ItalicClicked) },
            )
            ToolbarToggle(
                glyph = AppIcons.FormatUnderlined,
                contentDescription = stringResource(R.string.note_editor_underline_action),
                active = underlineActive,
                onClick = { onEvent(NoteEditorEvent.UnderlineClicked) },
            )
            Box {
                IconButton(
                    onClick = { onEvent(NoteEditorEvent.ColorPopoverOpened) },
                    modifier = Modifier.focusProperties { canFocus = false },
                ) {
                    Box(
                        modifier = Modifier
                            .size(COLOR_DOT_SIZE)
                            .background(color = activeColor ?: LocalContentColor.current, shape = CircleShape),
                    )
                }
                ColorPopover(expanded = uiState.isColorPopoverVisible, activeColor = activeColor, onEvent = onEvent)
            }
            ToolbarDivider(testTag = TOOLBAR_COLOR_DIVIDER_TAG)
            Box {
                IconButton(
                    onClick = { onEvent(NoteEditorEvent.HeadingPopoverOpened) },
                    modifier = Modifier.focusProperties { canFocus = false },
                ) {
                    AppIcon(AppIcons.Title, contentDescription = stringResource(R.string.note_editor_heading_action))
                }
                HeadingPopover(expanded = uiState.isHeadingPopoverVisible, selected = focusedHeading, onEvent = onEvent)
            }
            ToolbarToggle(
                glyph = AppIcons.FormatListBulleted,
                contentDescription = stringResource(R.string.note_editor_bullet_action),
                active = bulletActive,
                onClick = { onEvent(NoteEditorEvent.BulletClicked) },
            )
            ToolbarToggle(
                glyph = AppIcons.FormatListNumbered,
                contentDescription = stringResource(R.string.note_editor_numbered_list_action),
                active = numberedListActive,
                onClick = { onEvent(NoteEditorEvent.NumberedListClicked) },
            )
            // A divider — rather than just trailing padding — gives the link button a visible left
            // edge instead of reading as a bare icon floating at the toolbar's far right.
            ToolbarDivider(testTag = TOOLBAR_LINK_DIVIDER_TAG)
            IconButton(
                onClick = { onEvent(NoteEditorEvent.LinkClicked) },
                modifier = Modifier.focusProperties { canFocus = false },
            ) {
                AppIcon(AppIcons.Link, contentDescription = stringResource(R.string.note_editor_link_action))
            }
        }
    }
}

private fun HeadingStyle?.toHeadingLevel(): HeadingLevel? = when (this) {
    HeadingStyle.H1 -> HeadingLevel.H1
    HeadingStyle.H2 -> HeadingLevel.H2
    HeadingStyle.H3 -> HeadingLevel.H3
    else -> null
}

internal const val NOTE_TITLE_FIELD_TAG = "noteTitleField"
internal const val NOTE_BODY_TAG = "noteBody"
internal const val TOOLBAR_COLOR_DIVIDER_TAG = "toolbarColorDivider"
internal const val TOOLBAR_LINK_DIVIDER_TAG = "toolbarLinkDivider"
internal const val LINK_DISPLAY_TEXT_FIELD_TAG = "linkDisplayTextField"
internal const val LINK_URL_FIELD_TAG = "linkUrlField"

private val COLOR_DOT_SIZE: Dp = 20.dp
private val DIVIDER_HEIGHT: Dp = 24.dp
private val TOOLBAR_CLEARANCE: Dp = 96.dp

@Composable
private fun ToolbarDivider(testTag: String) {
    VerticalDivider(
        modifier = Modifier
            .testTag(testTag)
            .padding(horizontal = JottiqSpacing.xs)
            .height(DIVIDER_HEIGHT),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

// A Material 3 IconToggleButton: its ripple is clipped to [shape] (unlike a plain Box +
// .clickable, whose default ripple ignores the background shape), and — using the fixed-shape
// overload rather than the morphing one — the checked state stays the same round shape as
// unchecked instead of swapping to a squarish one, which is what produced the "square flash" look.
@Composable
private fun ToolbarToggle(
    glyph: AppIconGlyph,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberJottiqHaptics()
    IconToggleButton(
        checked = active,
        onCheckedChange = {
            haptics.perform(if (active) JottiqHapticType.ToggleOff else JottiqHapticType.ToggleOn)
            onClick()
        },
        // Toolbar controls must not steal keyboard/input focus from the note's text field:
        // doing so ends and restarts its text-input session, which both collapses the
        // selection (breaking the next format toggle) and can drop already-applied formatting
        // spans when the platform hands the field's value back on refocus.
        modifier = Modifier.focusProperties { canFocus = false },
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        AppIcon(glyph, contentDescription = contentDescription)
    }
}

@Composable
private fun ColorPopover(expanded: Boolean, activeColor: Color?, onEvent: (NoteEditorEvent) -> Unit) {
    val haptics = rememberJottiqHaptics()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onEvent(NoteEditorEvent.ColorPopoverDismissed) },
        // DropdownMenu is a genuine separate popup window that, per its default PopupProperties,
        // is focusable — meaning it steals Android window focus from the note's text field the
        // moment it opens. That ends the field's text-input/IME session, which both collapses the
        // selection and can drop already-applied formatting spans when the platform hands the
        // field's value back on refocus. Non-focusable still dismisses fine on an outside tap.
        properties = PopupProperties(focusable = false),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = JottiqSpacing.m, vertical = JottiqSpacing.s),
            horizontalArrangement = Arrangement.spacedBy(JottiqSpacing.s),
        ) {
            ColorSwatch(
                color = LocalContentColor.current,
                selected = activeColor == null,
                contentDescription = stringResource(R.string.note_editor_color_default),
                onClick = {
                    haptics.perform(JottiqHapticType.Confirm)
                    onEvent(NoteEditorEvent.ColorSelected(NoteTextColor.Default))
                },
            )
            ColorSwatch(
                color = NoteTextColorPalette.Red,
                selected = activeColor == NoteTextColorPalette.Red,
                contentDescription = stringResource(R.string.note_editor_color_red),
                onClick = {
                    haptics.perform(JottiqHapticType.Confirm)
                    onEvent(NoteEditorEvent.ColorSelected(NoteTextColor.Red))
                },
            )
            ColorSwatch(
                color = NoteTextColorPalette.Blue,
                selected = activeColor == NoteTextColorPalette.Blue,
                contentDescription = stringResource(R.string.note_editor_color_blue),
                onClick = {
                    haptics.perform(JottiqHapticType.Confirm)
                    onEvent(NoteEditorEvent.ColorSelected(NoteTextColor.Blue))
                },
            )
            ColorSwatch(
                color = NoteTextColorPalette.Green,
                selected = activeColor == NoteTextColorPalette.Green,
                contentDescription = stringResource(R.string.note_editor_color_green),
                onClick = {
                    haptics.perform(JottiqHapticType.Confirm)
                    onEvent(NoteEditorEvent.ColorSelected(NoteTextColor.Green))
                },
            )
            ColorSwatch(
                color = NoteTextColorPalette.Gold,
                selected = activeColor == NoteTextColorPalette.Gold,
                contentDescription = stringResource(R.string.note_editor_color_gold),
                onClick = {
                    haptics.perform(JottiqHapticType.Confirm)
                    onEvent(NoteEditorEvent.ColorSelected(NoteTextColor.Gold))
                },
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, contentDescription: String, onClick: () -> Unit) {
    val ringModifier = if (selected) {
        Modifier.border(SWATCH_RING_WIDTH, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(SWATCH_SIZE)
            .then(ringModifier)
            .padding(SWATCH_RING_WIDTH)
            .background(color, CircleShape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    )
}

private val SWATCH_SIZE: Dp = 32.dp
private val SWATCH_RING_WIDTH: Dp = 2.dp

@Composable
private fun HeadingPopover(expanded: Boolean, selected: HeadingLevel?, onEvent: (NoteEditorEvent) -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onEvent(NoteEditorEvent.HeadingPopoverDismissed) },
        // See the matching comment on ColorPopover: keeps this popup from stealing window focus
        // (and corrupting the text field's formatting) away from the note's text field.
        properties = PopupProperties(focusable = false),
        // The menu surface itself is the segmented container from `design/08. Titles.png` — drawn
        // here via the DropdownMenu's own shape/containerColor, rather than the content Row also
        // painting a second, differently-aligned background inside it (two mismatched rounded
        // rects was the "background doesn't fit the popup" bug).
        shape = MaterialTheme.shapes.small,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        // Scaled H1 > H2 > H3 labels in a single row, matching `design/08. Titles.png`.
        Row(
            modifier = Modifier.padding(JottiqSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(JottiqSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeadingOption(
                level = HeadingLevel.H1,
                labelRes = R.string.note_editor_heading_h1,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                selected = selected,
                onEvent = onEvent,
            )
            HeadingOption(
                level = HeadingLevel.H2,
                labelRes = R.string.note_editor_heading_h2,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                selected = selected,
                onEvent = onEvent,
            )
            HeadingOption(
                level = HeadingLevel.H3,
                labelRes = R.string.note_editor_heading_h3,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                selected = selected,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun HeadingOption(
    level: HeadingLevel,
    @StringRes labelRes: Int,
    style: TextStyle,
    selected: HeadingLevel?,
    onEvent: (NoteEditorEvent) -> Unit,
) {
    val haptics = rememberJottiqHaptics()
    JottiqHeadingToggleButton(
        checked = selected == level,
        onCheckedChange = {
            // Selecting the already-active level un-toggles it back to Normal (see
            // NoteEditorViewModel.onHeadingSelected) — a real toggle, not just a single-choice pick.
            haptics.perform(if (selected == level) JottiqHapticType.ToggleOff else JottiqHapticType.ToggleOn)
            onEvent(NoteEditorEvent.HeadingSelected(level))
        },
    ) {
        Text(text = stringResource(labelRes), style = style)
    }
}

@Composable
private fun InsertLinkDialog(displayText: String, url: String, onEvent: (NoteEditorEvent) -> Unit) {
    val haptics = rememberJottiqHaptics()
    // Resolved here, not inside the content lambda below: AlertDialog's content slot is composed
    // in its own window, which re-provides LocalContext from the real Activity context, shadowing
    // LocalizedContent's in-app language override. stringResource() must run outside that boundary.
    val displayLabel = stringResource(R.string.note_editor_link_display_label)
    val urlPlaceholder = stringResource(R.string.note_editor_link_url_placeholder)
    JottiqInputDialog(
        title = stringResource(R.string.note_editor_link_dialog_title),
        confirmLabel = stringResource(R.string.note_editor_link_insert),
        dismissLabel = stringResource(R.string.note_editor_link_cancel),
        confirmEnabled = url.isNotBlank(),
        onConfirm = {
            haptics.perform(JottiqHapticType.Confirm)
            onEvent(NoteEditorEvent.LinkInsertConfirmed)
        },
        onDismiss = { onEvent(NoteEditorEvent.LinkDialogDismissed) },
    ) {
        JottiqDialogTextField(
            value = displayText,
            onValueChange = { onEvent(NoteEditorEvent.LinkDisplayTextChanged(it)) },
            placeholder = displayLabel,
            modifier = Modifier.testTag(LINK_DISPLAY_TEXT_FIELD_TAG),
        )
        JottiqDialogTextField(
            value = url,
            onValueChange = { onEvent(NoteEditorEvent.LinkUrlChanged(it)) },
            placeholder = urlPlaceholder,
            modifier = Modifier
                .padding(top = JottiqSpacing.s)
                .testTag(LINK_URL_FIELD_TAG),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToFolderSheet(
    folders: List<MoveFolderRowUi>,
    selectedFolderId: String?,
    onEvent: (NoteEditorEvent) -> Unit,
) {
    val haptics = rememberJottiqHaptics()
    ModalBottomSheet(onDismissRequest = { onEvent(NoteEditorEvent.MoveSheetDismissed) }) {
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
                    onClick = { onEvent(NoteEditorEvent.MoveFolderSelected(row.id)) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(JottiqSpacing.l),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { onEvent(NoteEditorEvent.MoveSheetDismissed) }) {
                Text(stringResource(R.string.move_sheet_cancel))
            }
            Button(
                onClick = {
                    haptics.perform(JottiqHapticType.Confirm)
                    onEvent(NoteEditorEvent.MoveConfirmed)
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

@Composable
private fun DeleteConfirmationDialog(title: String, onEvent: (NoteEditorEvent) -> Unit) {
    val haptics = rememberJottiqHaptics()
    val displayTitle = title.ifBlank { stringResource(R.string.untitled_note) }
    AlertDialog(
        onDismissRequest = { onEvent(NoteEditorEvent.DeleteDialogDismissed) },
        title = { Text(stringResource(R.string.note_editor_delete_dialog_title)) },
        text = { Text(stringResource(R.string.note_editor_delete_dialog_body, displayTitle)) },
        confirmButton = {
            Button(
                onClick = {
                    haptics.perform(JottiqHapticType.Reject)
                    onEvent(NoteEditorEvent.DeleteConfirmed)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.note_editor_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(NoteEditorEvent.DeleteDialogDismissed) }) {
                Text(stringResource(R.string.note_editor_delete_dialog_cancel))
            }
        },
    )
}

@Preview(name = "Read - Light", showBackground = true)
@Composable
private fun NoteEditorScreenPreviewReadLight() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        NoteEditorContent(uiState = previewUiState(isEditing = false), onEvent = {})
    }
}

@Preview(name = "Read - Dark", showBackground = true)
@Composable
private fun NoteEditorScreenPreviewReadDark() {
    JottiqTheme(darkTheme = true, dynamicColor = false) {
        NoteEditorContent(uiState = previewUiState(isEditing = false), onEvent = {})
    }
}

@Preview(name = "Edit - Light", showBackground = true)
@Composable
private fun NoteEditorScreenPreviewEditLight() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        NoteEditorContent(uiState = previewUiState(isEditing = true), onEvent = {})
    }
}

@Preview(name = "New note - Light", showBackground = true)
@Composable
private fun NoteEditorScreenPreviewNewNoteLight() {
    JottiqTheme(darkTheme = false, dynamicColor = false) {
        NoteEditorContent(uiState = newNotePreviewUiState(), onEvent = {})
    }
}

private fun newNotePreviewUiState() = NoteEditorUiState(
    isLoading = false,
    title = "",
    isEditing = true,
    isFavorite = false,
    dateLabel = "14:02",
    wasEdited = false,
    segments = listOf(EditorSegment.Rich(id = "p1", state = newRichTextState())),
)

private fun previewUiState(isEditing: Boolean) = NoteEditorUiState(
    isLoading = false,
    title = "Groceries",
    isEditing = isEditing,
    isFavorite = true,
    dateLabel = "14:02",
    wasEdited = true,
    segments = listOf(
        EditorSegment.Rich(
            id = "p1",
            state = newRichTextState(
                "<p>For Saturday &#8212; <b>farmers market</b> first, then the co-op.</p>" +
                    "<p>Milk (oat)</p><p>Eggs</p>",
            ),
        ),
    ),
)
