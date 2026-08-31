package com.anacardix.jottiq.ui.noteeditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anacardix.jottiq.R
import com.anacardix.jottiq.di.DefaultDispatcher
import com.anacardix.jottiq.domain.DataResult
import com.anacardix.jottiq.domain.FoldersRepository
import com.anacardix.jottiq.domain.HeadingLevel
import com.anacardix.jottiq.domain.Note
import com.anacardix.jottiq.domain.NoteBlock
import com.anacardix.jottiq.domain.NoteTextColor
import com.anacardix.jottiq.domain.NotesRepository
import com.anacardix.jottiq.domain.SettingsRepository
import com.anacardix.jottiq.domain.toLocale
import com.anacardix.jottiq.domain.usecase.BuildFolderTreeUseCase
import com.anacardix.jottiq.domain.usecase.FormatRelativeDateUseCase
import com.anacardix.jottiq.ui.common.MoveFolderRowUi
import com.anacardix.jottiq.ui.common.ROOT_FOLDER_ID
import com.anacardix.jottiq.ui.common.UserMessage
import com.anacardix.jottiq.ui.navigation.NoteRoute
import com.mohamedrejeb.richeditor.model.HeadingStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** How long typing may pause before the note is autosaved (Done/Back always flush immediately). */
internal const val AUTOSAVE_DEBOUNCE_MS = 400L

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions") // Hilt constructor injection; one handler per NoteEditorEvent case
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val notesRepository: NotesRepository,
    private val foldersRepository: FoldersRepository,
    private val settingsRepository: SettingsRepository,
    private val formatRelativeDate: FormatRelativeDateUseCase,
    private val buildFolderTree: BuildFolderTreeUseCase,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val route: NoteRoute = savedStateHandle.toRoute()
    private val noteId: String = route.noteId

    private val _uiState = MutableStateFlow(NoteEditorUiState(noteId = noteId))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val navigationChannel = Channel<NoteEditorNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NoteEditorNavigationEvent> = navigationChannel.receiveAsFlow()

    private var hasLoaded = false
    private var loadedNote: Note? = null

    // True when [noteId] had no title and no blocks the moment it was loaded — i.e. it's the
    // just-inserted scaffold row from NotesRepositoryImpl.createNote(), not a note the user ever
    // put content into. Drives the hard-delete-vs-trash choice in deleteCurrentNote().
    private var isNewNote = false

    // Autosave bookkeeping: [dirty] means the ui state has edits the repository hasn't seen yet;
    // [discarded] permanently disarms autosave once an empty note has been moved to trash on back,
    // so a still-pending debounced write can't resurrect it.
    private var persistJob: Job? = null
    private var dirty = false
    private var discarded = false

    // Segment ids whose RichTextState content changed since the last successful persist, and the
    // blocks that persist last produced for each still-live segment id. doPersist() reuses a
    // segment's cached blocks (skipping its HTML round-trip) unless its id is here — see
    // NoteDocumentBridge.toNoteDocumentCached. A structural edit (applyEdit) invalidates the whole
    // cache instead of reasoning about which ids stayed equivalent across the reshape.
    private val dirtySegmentIds = mutableSetOf<String>()
    private var cachedBlocksBySegmentId: Map<String, List<NoteBlock>> = emptyMap()

    @Suppress("CyclomaticComplexMethod")
    fun onEvent(event: NoteEditorEvent) {
        when (event) {
            NoteEditorEvent.ScreenShown -> loadIfNeeded()
            NoteEditorEvent.BackClicked -> onBackClicked()
            is NoteEditorEvent.EditModeRequested -> onEditModeRequested(event.segmentId)
            is NoteEditorEvent.TitleChanged -> onTitleChanged(event.title)
            NoteEditorEvent.TitleNextPressed -> onTitleNextPressed()
            is NoteEditorEvent.SegmentFocusChanged -> _uiState.update { it.copy(focusedSegmentId = event.segmentId) }
            NoteEditorEvent.FocusRequestConsumed -> _uiState.update { it.copy(pendingFocus = null) }
            is NoteEditorEvent.SegmentContentChanged -> onSegmentContentChanged(event.segmentId)
            NoteEditorEvent.BoldClicked -> onToggleStyle(SpanStyle(fontWeight = FontWeight.Bold))
            NoteEditorEvent.ItalicClicked -> onToggleStyle(SpanStyle(fontStyle = FontStyle.Italic))
            NoteEditorEvent.UnderlineClicked -> onToggleUnderline()
            NoteEditorEvent.ColorPopoverOpened -> _uiState.update { it.copy(isColorPopoverVisible = true) }
            NoteEditorEvent.ColorPopoverDismissed -> _uiState.update { it.copy(isColorPopoverVisible = false) }
            is NoteEditorEvent.ColorSelected -> onColorSelected(event.color)
            NoteEditorEvent.HeadingPopoverOpened -> _uiState.update { it.copy(isHeadingPopoverVisible = true) }
            NoteEditorEvent.HeadingPopoverDismissed -> _uiState.update { it.copy(isHeadingPopoverVisible = false) }
            is NoteEditorEvent.HeadingSelected -> onHeadingSelected(event.heading)
            NoteEditorEvent.BulletClicked -> onBulletClicked()
            NoteEditorEvent.NumberedListClicked -> onNumberedListClicked()
            NoteEditorEvent.LinkClicked -> onLinkClicked()
            is NoteEditorEvent.LinkDisplayTextChanged -> _uiState.update { it.copy(linkDisplayText = event.text) }
            is NoteEditorEvent.LinkUrlChanged -> _uiState.update { it.copy(linkUrl = event.url) }
            NoteEditorEvent.LinkDialogDismissed -> dismissLinkDialog()
            NoteEditorEvent.LinkInsertConfirmed -> onLinkInsertConfirmed()
            NoteEditorEvent.FavoriteClicked -> onFavoriteClicked()
            NoteEditorEvent.LockClicked -> onLockClicked()
            NoteEditorEvent.MoveClicked -> onMoveClicked()
            is NoteEditorEvent.MoveFolderSelected -> onMoveFolderSelected(event.folderId)
            NoteEditorEvent.MoveConfirmed -> onMoveConfirmed()
            NoteEditorEvent.MoveSheetDismissed -> dismissMoveSheet()
            NoteEditorEvent.DeleteClicked -> _uiState.update { it.copy(isDeleteDialogVisible = true) }
            NoteEditorEvent.DeleteConfirmed -> onDeleteConfirmed()
            NoteEditorEvent.DeleteDialogDismissed -> _uiState.update { it.copy(isDeleteDialogVisible = false) }
            NoteEditorEvent.UserMessageShown -> _uiState.update { it.copy(userMessage = null) }
        }
    }

    private fun loadIfNeeded() {
        if (hasLoaded) return
        hasLoaded = true
        viewModelScope.launch {
            val note = notesRepository.observeNoteById(noteId).filterNotNull().first()
            loadedNote = note
            // A brand-new note has no blocks and no title yet — open straight into edit mode with
            // the caret in the title, instead of an apparently blank screen.
            isNewNote = note.document.blocks.isEmpty() && note.title.isEmpty()
            val wasEdited = note.updatedAt != note.createdAt
            val locale = settingsRepository.observeLanguage().first().toLocale()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    title = note.title,
                    isEditing = it.isEditing || isNewNote,
                    isFavorite = note.isFavorite,
                    isLocked = note.isLocked,
                    dateLabel = formatRelativeDate.formatEditorTimestamp(
                        if (wasEdited) note.updatedAt else note.createdAt,
                        locale,
                    ),
                    wasEdited = wasEdited,
                    segments = note.document.toSegments(),
                    pendingFocus = if (isNewNote) EditorFocusTarget.Title else it.pendingFocus,
                )
            }
        }
    }

    // Tapping read-mode content starts editing at that segment; tapping past the end of the note
    // (segmentId == null) drops the caret into the last segment — both Apple Notes behaviors.
    private fun onEditModeRequested(segmentId: String?) {
        _uiState.update { state ->
            val target = segmentId ?: state.segments.lastOrNull()?.id
            state.copy(isEditing = true, pendingFocus = target?.let { EditorFocusTarget.Segment(it) })
        }
    }

    private fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
        schedulePersist()
    }

    private fun onTitleNextPressed() {
        _uiState.update { state ->
            state.copy(pendingFocus = state.segments.firstOrNull()?.let { EditorFocusTarget.Segment(it.id) })
        }
    }

    // With a real selection, this toggles [style] over that range immediately; with just a cursor,
    // RichTextState arms it as the "type-ahead" style for the next typed characters on its own.
    private fun onToggleStyle(style: SpanStyle) {
        val segment = focusedSegment() ?: return
        segment.state.toggleSpanStyle(style)
        persistNow(segment.id)
    }

    // Underline visibly smears across an emoji glyph, so — unlike bold/italic, which are invisible
    // on emoji — a real (non-collapsed) selection is split into its non-emoji sub-ranges (see
    // [nonEmojiSubRanges]) and each is toggled individually, leaving any emoji in the selection
    // unformatted. A collapsed cursor keeps the plain type-ahead toggle behavior.
    private fun onToggleUnderline() {
        val segment = focusedSegment() ?: return
        val style = SpanStyle(textDecoration = TextDecoration.Underline)
        val selection = segment.state.selection
        if (selection.collapsed) {
            segment.state.toggleSpanStyle(style)
        } else {
            val isActive = segment.state.currentSpanStyle.textDecoration == TextDecoration.Underline
            val ranges = nonEmojiSubRanges(segment.state.annotatedString.text, selection)
            applyOverRanges(segment, selection, ranges) {
                if (isActive) segment.state.removeSpanStyle(style) else segment.state.addSpanStyle(style)
            }
        }
        persistNow(segment.id)
    }

    private fun onColorSelected(color: NoteTextColor) {
        _uiState.update { it.copy(isColorPopoverVisible = false) }
        val segment = focusedSegment() ?: return
        val target = NoteTextColorPalette.colorFor(color)
        val selection = segment.state.selection
        if (selection.collapsed) {
            if (target != null) {
                segment.state.addSpanStyle(SpanStyle(color = target))
            } else {
                // removeSpanStyle matches on the exact color value, unlike toggleSpanStyle's
                // presence-based matching for bold/italic/underline — so "clear to Default" must
                // pass whichever color is actually active, not an arbitrary placeholder.
                val active = segment.state.currentSpanStyle.color
                if (active != Color.Unspecified) segment.state.removeSpanStyle(SpanStyle(color = active))
            }
        } else {
            // Same emoji-exclusion as onToggleUnderline: color visibly smears across emoji too.
            val ranges = nonEmojiSubRanges(segment.state.annotatedString.text, selection)
            if (target != null) {
                applyOverRanges(segment, selection, ranges) { segment.state.addSpanStyle(SpanStyle(color = target)) }
            } else {
                val active = segment.state.currentSpanStyle.color
                if (active != Color.Unspecified) {
                    applyOverRanges(segment, selection, ranges) {
                        segment.state.removeSpanStyle(SpanStyle(color = active))
                    }
                }
            }
        }
        persistNow(segment.id)
    }

    // Applies [apply] once per range in [ranges], temporarily pointing the segment's selection at
    // each range so the library's addSpanStyle/removeSpanStyle operate on it, then restores
    // [originalSelection]. Used to skip emoji grapheme clusters within a formatting selection.
    private fun applyOverRanges(
        segment: EditorSegment,
        originalSelection: TextRange,
        ranges: List<TextRange>,
        apply: () -> Unit,
    ) {
        ranges.forEach { subRange ->
            segment.state.selection = subRange
            apply()
        }
        segment.state.selection = originalSelection
    }

    // Toggles the library's own native per-paragraph HeadingStyle in place — read mode renders this
    // same RichTextState through the library's own BasicRichText (see NoteReadText.kt), so no extra
    // size override is needed for read mode to match.
    private fun onHeadingSelected(heading: HeadingLevel) {
        _uiState.update { it.copy(isHeadingPopoverVisible = false) }
        val segment = focusedSegment() as? EditorSegment.Rich ?: return
        val target = heading.toHeadingStyle()
        segment.state.setHeadingStyle(if (segment.state.currentHeadingStyle == target) HeadingStyle.Normal else target)
        persistNow(segment.id)
    }

    // Toolbar bullet toggle: a focused rich line toggles its own bullet state in place.
    private fun onBulletClicked() {
        val segment = focusedSegment() as? EditorSegment.Rich ?: return
        segment.state.toggleUnorderedList()
        persistNow(segment.id)
    }

    // Toolbar numbered-list toggle: a focused rich line toggles its own ordered-list state in
    // place. Mirrors onBulletClicked; the library's toggleOrderedList() replaces any existing
    // bullet on the same line, so the two toolbar controls stay mutually exclusive for free.
    private fun onNumberedListClicked() {
        val segment = focusedSegment() as? EditorSegment.Rich ?: return
        segment.state.toggleOrderedList()
        persistNow(segment.id)
    }

    private fun onLinkClicked() {
        val segment = focusedSegment()
        val prefill = segment?.state?.let { state ->
            state.takeUnless { it.selection.collapsed }
                ?.let { it.annotatedString.text.substring(it.selection.min, it.selection.max) }
        }.orEmpty()
        _uiState.update { it.copy(isLinkDialogVisible = true, linkDisplayText = prefill, linkUrl = "") }
    }

    private fun dismissLinkDialog() {
        _uiState.update { it.copy(isLinkDialogVisible = false, linkDisplayText = "", linkUrl = "") }
    }

    private fun onLinkInsertConfirmed() {
        val rawUrl = _uiState.value.linkUrl.trim()
        if (rawUrl.isEmpty()) return
        // The link target is normalized (scheme added if missing) so it always resolves; the
        // display text keeps whatever the user actually typed, falling back to the raw URL.
        val displayText = _uiState.value.linkDisplayText.ifBlank { rawUrl }
        val url = normalizeLinkUrl(rawUrl)
        // Fall back to the end of the note when no segment is focused, so confirming the dialog
        // never silently drops the link.
        val segment = focusedSegment() ?: _uiState.value.segments.lastOrNull()
        if (segment != null) {
            val insertStart = segment.state.selection.min
            if (segment.state.selection.collapsed) {
                segment.state.addTextAtIndex(insertStart, displayText)
            } else {
                segment.state.replaceSelectedText(displayText)
            }
            segment.state.addLinkToTextRange(url, TextRange(insertStart, insertStart + displayText.length))
        }
        dismissLinkDialog()
        persistNow(segment?.id)
    }

    // Unlike content edits, favoriting goes straight to NotesRepository.setFavorite instead of the
    // general doPersist() path — it's a metadata toggle that must not bump updatedAt (only writing
    // content or changing the title does).
    private fun onFavoriteClicked() {
        val favorite = !_uiState.value.isFavorite
        _uiState.update { it.copy(isFavorite = favorite) }
        viewModelScope.launch {
            when (notesRepository.setFavorite(noteId, favorite)) {
                is DataResult.Success -> loadedNote = loadedNote?.copy(isFavorite = favorite)
                is DataResult.Failure -> _uiState.update {
                    it.copy(isFavorite = !favorite, userMessage = UserMessage(R.string.note_editor_error_save))
                }
            }
        }
    }

    // Unlike content edits, locking goes straight to NotesRepository.setLocked instead of the
    // general doPersist() path — it's a metadata toggle that must not bump updatedAt (only writing
    // content or changing the title does).
    private fun onLockClicked() {
        val locked = !_uiState.value.isLocked
        _uiState.update { it.copy(isLocked = locked) }
        viewModelScope.launch {
            when (notesRepository.setLocked(noteId, locked)) {
                is DataResult.Success -> Unit
                is DataResult.Failure -> _uiState.update {
                    it.copy(isLocked = !locked, userMessage = UserMessage(R.string.note_editor_error_save))
                }
            }
        }
    }

    private fun onMoveClicked() {
        val currentFolderId = loadedNote?.folderId ?: ROOT_FOLDER_ID
        viewModelScope.launch {
            val folders = foldersRepository.observeActiveFolders().first()
            val rows = buildFolderTree(folders).map { row ->
                MoveFolderRowUi(
                    id = row.id,
                    name = row.name,
                    depth = row.depth,
                    isLocked = row.isLocked,
                    isCurrent = row.id == currentFolderId,
                )
            }
            val rootRow = MoveFolderRowUi(
                id = ROOT_FOLDER_ID,
                name = "",
                depth = 0,
                isLocked = false,
                isCurrent = currentFolderId == ROOT_FOLDER_ID,
            )
            _uiState.update {
                it.copy(isMoveSheetVisible = true, moveFolders = listOf(rootRow) + rows, selectedMoveFolderId = null)
            }
        }
    }

    private fun onMoveFolderSelected(folderId: String) {
        val row = _uiState.value.moveFolders.firstOrNull { it.id == folderId } ?: return
        if (row.isCurrent) return
        _uiState.update { it.copy(selectedMoveFolderId = folderId) }
    }

    private fun onMoveConfirmed() {
        val selected = _uiState.value.selectedMoveFolderId ?: return
        val base = loadedNote ?: return
        val newFolderId = selected.takeUnless { it == ROOT_FOLDER_ID }
        viewModelScope.launch {
            when (notesRepository.setFolder(base.id, newFolderId)) {
                is DataResult.Success -> {
                    loadedNote = base.copy(folderId = newFolderId)
                    val message = if (selected == ROOT_FOLDER_ID) {
                        UserMessage(R.string.note_editor_moved_to_root)
                    } else {
                        val folderName = _uiState.value.moveFolders.firstOrNull { it.id == selected }?.name.orEmpty()
                        UserMessage(R.string.note_editor_moved_to_folder, listOf(folderName))
                    }
                    _uiState.update {
                        it.copy(
                            isMoveSheetVisible = false,
                            moveFolders = emptyList(),
                            selectedMoveFolderId = null,
                            userMessage = message,
                        )
                    }
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(userMessage = UserMessage(R.string.note_editor_error_save))
                }
            }
        }
    }

    private fun dismissMoveSheet() {
        _uiState.update { it.copy(isMoveSheetVisible = false, moveFolders = emptyList(), selectedMoveFolderId = null) }
    }

    private fun onBackClicked() {
        // Only ever discard a note once it has actually finished loading — otherwise the
        // still-default (blank) placeholder state would look "empty" and wrongly trash a real,
        // already-loaded-elsewhere note if the user backs out before the load completes.
        val canDiscard = !_uiState.value.isLoading && isCurrentNoteEmpty()
        persistJob?.cancel()
        viewModelScope.launch {
            if (canDiscard) {
                // Disarm autosave first: a debounced write landing after deleteCurrentNote() would
                // resurrect the note as a blank row on Home.
                discarded = true
                deleteCurrentNote()
            } else if (dirty) {
                doPersist()
            }
            navigationChannel.trySend(NoteEditorNavigationEvent.Back)
        }
    }

    // A note with no title and no typed content is discarded instead of left behind as a blank row
    // on Home; see deleteCurrentNote() for whether that means a hard delete or a trash entry.
    private fun isCurrentNoteEmpty(): Boolean {
        val state = _uiState.value
        return state.title.isBlank() && state.segments.all { it.state.annotatedString.text.isBlank() }
    }

    // A note the user never put any content into — created, then backed out of or deleted while
    // still blank — never held real data, so it's hard-deleted outright instead of leaving an empty
    // row in Trash (the one exception to CLAUDE.md's "hard-delete only in trash purge": there is no
    // user data to preserve). Any other note, including one that had content and was cleared back to
    // blank, still goes through the normal soft-delete trash path.
    private suspend fun deleteCurrentNote(): DataResult<Unit> =
        if (isNewNote && isCurrentNoteEmpty()) {
            notesRepository.discardBlankNote(noteId)
        } else {
            notesRepository.moveToTrash(noteId)
        }

    private fun onDeleteConfirmed() {
        persistJob?.cancel()
        viewModelScope.launch {
            when (deleteCurrentNote()) {
                is DataResult.Success -> {
                    discarded = true
                    _uiState.update { it.copy(isDeleteDialogVisible = false) }
                    navigationChannel.trySend(NoteEditorNavigationEvent.Back)
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(isDeleteDialogVisible = false, userMessage = UserMessage(R.string.note_editor_error_save))
                }
            }
        }
    }

    private fun focusedSegment(): EditorSegment? =
        _uiState.value.segments.firstOrNull { it.id == _uiState.value.focusedSegmentId }

    // Splitting a paragraph on Enter resets the new paragraph's HeadingStyle to Normal natively
    // (library-native, see RichTextState.checkForParagraphs), but with config.preserveStyleOnEmptyLine
    // (NoteDocumentBridge.newRichTextState) the raw bold+oversized SpanStyle the heading rendered
    // with carries over onto the new paragraph's type-ahead style regardless, so typing right after
    // Enter still looks and reads like the heading. Editor v1 has no manual font-size control, so a
    // specified fontSize here can only be that heading residue -- strip it (and the heading's bold)
    // once the paragraph is confirmed no longer a heading, matching how removeSpanStyle is already
    // used to clear an exact prior color in onColorSelected.
    private fun onSegmentContentChanged(segmentId: String) {
        val segment = _uiState.value.segments.firstOrNull { it.id == segmentId } as? EditorSegment.Rich
        if (segment != null && segment.state.selection.collapsed) {
            val state = segment.state
            val fontSize = state.currentSpanStyle.fontSize
            if (state.currentHeadingStyle == HeadingStyle.Normal && fontSize != TextUnit.Unspecified) {
                state.removeSpanStyle(SpanStyle(fontSize = fontSize, fontWeight = FontWeight.Bold))
            }
        }
        schedulePersist(segmentId)
    }

    // Typing autosaves after a short pause instead of on every keystroke: serializing the whole
    // document (per-segment HTML round-trip) plus a Room write per character is what made the
    // editor feel sluggish. Rare, discrete edits (toggles, splits, checkbox taps) persist
    // immediately, and Done/Back flush whatever is still pending. [touchedSegmentId], when given,
    // marks that one segment's cached blocks stale for doPersist()'s cached partial reserialize.
    private fun schedulePersist(touchedSegmentId: String? = null) {
        touchedSegmentId?.let { dirtySegmentIds += it }
        dirty = true
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            doPersist()
        }
    }

    private fun persistNow(touchedSegmentId: String? = null) {
        touchedSegmentId?.let { dirtySegmentIds += it }
        dirty = true
        flushPersist()
    }

    private fun flushPersist() {
        if (!dirty) return
        persistJob?.cancel()
        persistJob = viewModelScope.launch { doPersist() }
    }

    private suspend fun doPersist() {
        if (discarded || !dirty) return
        val base = loadedNote ?: return
        dirty = false
        val dirtyIds = dirtySegmentIds.toSet()
        dirtySegmentIds.clear()
        val current = _uiState.value
        // The HTML round-trip (toNoteDocumentCached) is CPU-bound, so it runs off Main; segments'
        // RichTextState is only read here (toHtml()/annotatedString), never mutated, which is safe
        // from a background dispatcher.
        val (document, freshCache) = withContext(defaultDispatcher) {
            current.segments.toNoteDocumentCached(cachedBlocksBySegmentId, dirtyIds)
        }
        cachedBlocksBySegmentId = freshCache
        val note = base.copy(
            title = current.title,
            document = document,
            isFavorite = current.isFavorite,
            isLocked = current.isLocked,
        )
        when (notesRepository.updateNote(note)) {
            is DataResult.Success -> loadedNote = note
            is DataResult.Failure -> _uiState.update {
                it.copy(userMessage = UserMessage(R.string.note_editor_error_save))
            }
        }
    }
}

private fun HeadingLevel.toHeadingStyle(): HeadingStyle = when (this) {
    HeadingLevel.H1 -> HeadingStyle.H1
    HeadingLevel.H2 -> HeadingStyle.H2
    HeadingLevel.H3 -> HeadingStyle.H3
}
