package com.anacardix.jottiq.ui.noteeditor

import com.anacardix.jottiq.domain.HeadingLevel
import com.anacardix.jottiq.domain.NoteTextColor

/** Intents for [NoteEditorScreen]. */
sealed interface NoteEditorEvent {
    data object ScreenShown : NoteEditorEvent
    data object BackClicked : NoteEditorEvent

    /** Tap on read-mode content; [segmentId] is the tapped segment (null = below the content). */
    data class EditModeRequested(val segmentId: String? = null) : NoteEditorEvent
    data class TitleChanged(val title: String) : NoteEditorEvent

    /** IME Next on the title field — moves the caret into the first body segment (Apple Notes). */
    data object TitleNextPressed : NoteEditorEvent
    data class SegmentFocusChanged(val segmentId: String) : NoteEditorEvent
    data object FocusRequestConsumed : NoteEditorEvent

    /** Fired whenever a focused [EditorSegment]'s [com.mohamedrejeb.richeditor.model.RichTextState]
     * mutates (text or formatting) — the state already mutated itself; this only schedules autosave,
     * marking [segmentId] as the one segment whose persisted content is now stale. */
    data class SegmentContentChanged(val segmentId: String) : NoteEditorEvent
    data object BoldClicked : NoteEditorEvent
    data object ItalicClicked : NoteEditorEvent
    data object UnderlineClicked : NoteEditorEvent
    data object ColorPopoverOpened : NoteEditorEvent
    data object ColorPopoverDismissed : NoteEditorEvent
    data class ColorSelected(val color: NoteTextColor) : NoteEditorEvent
    data object HeadingPopoverOpened : NoteEditorEvent
    data object HeadingPopoverDismissed : NoteEditorEvent
    data class HeadingSelected(val heading: HeadingLevel) : NoteEditorEvent
    data object BulletClicked : NoteEditorEvent
    data object NumberedListClicked : NoteEditorEvent
    data object LinkClicked : NoteEditorEvent
    data class LinkDisplayTextChanged(val text: String) : NoteEditorEvent
    data class LinkUrlChanged(val url: String) : NoteEditorEvent
    data object LinkDialogDismissed : NoteEditorEvent
    data object LinkInsertConfirmed : NoteEditorEvent
    data object FavoriteClicked : NoteEditorEvent
    data object LockClicked : NoteEditorEvent
    data object MoveClicked : NoteEditorEvent
    data class MoveFolderSelected(val folderId: String) : NoteEditorEvent
    data object MoveConfirmed : NoteEditorEvent
    data object MoveSheetDismissed : NoteEditorEvent
    data object DeleteClicked : NoteEditorEvent
    data object DeleteConfirmed : NoteEditorEvent
    data object DeleteDialogDismissed : NoteEditorEvent
    data object UserMessageShown : NoteEditorEvent
}
