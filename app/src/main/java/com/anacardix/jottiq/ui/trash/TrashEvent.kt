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
}
