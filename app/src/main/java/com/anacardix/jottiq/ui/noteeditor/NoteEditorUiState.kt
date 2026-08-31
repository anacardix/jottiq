package com.anacardix.jottiq.ui.noteeditor

import com.anacardix.jottiq.ui.common.MoveFolderRowUi
import com.anacardix.jottiq.ui.common.UserMessage

/** UI state for [NoteEditorScreen], modeled on `design/04. Note.png` through `09. Add link.png`. */
data class NoteEditorUiState(
    val isLoading: Boolean = true,
    val noteId: String = "",
    val title: String = "",
    val isEditing: Boolean = false,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val dateLabel: String = "",
    val wasEdited: Boolean = false,
    val segments: List<EditorSegment> = emptyList(),
    val focusedSegmentId: String? = null,
    // Set whenever the ViewModel wants the keyboard caret somewhere specific (a freshly split
    // segment, the title of a brand-new note, ...); cleared once the composable has moved
    // focus there (see NoteEditorEvent.FocusRequestConsumed).
    val pendingFocus: EditorFocusTarget? = null,
    val isColorPopoverVisible: Boolean = false,
    val isHeadingPopoverVisible: Boolean = false,
    val isLinkDialogVisible: Boolean = false,
    val linkDisplayText: String = "",
    val linkUrl: String = "",
    val isMoveSheetVisible: Boolean = false,
    val moveFolders: List<MoveFolderRowUi> = emptyList(),
    val selectedMoveFolderId: String? = null,
    val isDeleteDialogVisible: Boolean = false,
    val userMessage: UserMessage? = null,
)

/** Where [NoteEditorUiState.pendingFocus] wants the keyboard caret to land. */
sealed interface EditorFocusTarget {
    data object Title : EditorFocusTarget
    data class Segment(val id: String) : EditorFocusTarget
}

/**
 * One editable region of the note body, backed by a live [com.mohamedrejeb.richeditor.model.RichTextState]
 * controller rather than an immutable value — the library owns cursor/selection/formatting/undo
 * state internally and mutates it imperatively (`toggleSpanStyle`, `setHeadingStyle`, ...), so
 * [state] identity must stay stable across recompositions instead of being rebuilt from a `.copy()`
 * like the rest of [NoteEditorUiState]. See [NoteDocumentBridge] for the [state] content bridge.
 */
sealed interface EditorSegment {
    val id: String
    val state: com.mohamedrejeb.richeditor.model.RichTextState

    /** A contiguous run of paragraphs/headings/bullet-list items — one native rich-text region. */
    data class Rich(
        override val id: String,
        override val state: com.mohamedrejeb.richeditor.model.RichTextState,
    ) : EditorSegment
}
