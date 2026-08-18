package com.anacardix.jottiq.ui.trash

/** Intents for [TrashScreen]. */
sealed interface TrashEvent {
    data object ScreenShown : TrashEvent
    data object BackClicked : TrashEvent
    data object EmptyTrashClicked : TrashEvent
    data object EmptyTrashConfirmed : TrashEvent
    data object EmptyTrashDialogDismissed : TrashEvent
    data class RestoreClicked(val id: String) : TrashEvent
    data object UserMessageShown : TrashEvent

    // Multi-select (long-press a row to enter, tap to toggle further rows).
    data class ItemLongPressed(val id: String) : TrashEvent
    data class SelectionToggled(val id: String) : TrashEvent
    data object SelectAllClicked : TrashEvent
    data object SelectionCancelled : TrashEvent
    data object RestoreSelectedClicked : TrashEvent
    data object DeleteForeverSelectedClicked : TrashEvent
    data object DeleteForeverConfirmed : TrashEvent
    data object DeleteForeverDialogDismissed : TrashEvent
}
