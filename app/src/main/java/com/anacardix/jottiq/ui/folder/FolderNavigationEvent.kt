package com.anacardix.jottiq.ui.folder

/** One-off navigation intents raised by [FolderViewModel], consumed once by [FolderScreen]. */
sealed interface FolderNavigationEvent {
    data class ToNote(val noteId: String) : FolderNavigationEvent
    data class ToFolder(val folderId: String) : FolderNavigationEvent
    data class ToLockedNote(val noteId: String, val title: String) : FolderNavigationEvent
    data class ToLockedFolder(val folderId: String, val name: String) : FolderNavigationEvent
}
