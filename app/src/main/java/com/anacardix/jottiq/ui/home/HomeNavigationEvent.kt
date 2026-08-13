package com.anacardix.jottiq.ui.home

/** One-off navigation intents raised by [HomeViewModel], consumed once by [HomeScreen]. */
sealed interface HomeNavigationEvent {
    data class ToNote(val noteId: String) : HomeNavigationEvent
    data class ToFolder(val folderId: String) : HomeNavigationEvent
    data class ToLockedNote(val noteId: String, val title: String) : HomeNavigationEvent
    data class ToLockedFolder(val folderId: String, val name: String) : HomeNavigationEvent
    data object ToTrash : HomeNavigationEvent
    data object ToSettings : HomeNavigationEvent
}
