package com.anacardix.jottiq.ui.trash

/** One-off navigation intents raised by [TrashViewModel], consumed once by [TrashScreen]. */
sealed interface TrashNavigationEvent {
    data object Back : TrashNavigationEvent
}
