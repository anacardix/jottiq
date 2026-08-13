package com.anacardix.jottiq.ui.noteeditor

/**
 * One-off navigation intents raised by [NoteEditorViewModel], consumed once by [NoteEditorScreen].
 * Move and Delete are both handled entirely in-screen via a
 * [ModalBottomSheet][androidx.compose.material3.ModalBottomSheet]/`AlertDialog` — after a
 * successful delete, [Back] is sent to leave the (now-trashed) note.
 */
sealed interface NoteEditorNavigationEvent {
    data object Back : NoteEditorNavigationEvent
}
