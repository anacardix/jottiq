package com.anacardix.jottiq.ui.unlockgate

/** One-off navigation intents raised by [UnlockGateViewModel], consumed once by [UnlockGateScreen]. */
sealed interface UnlockGateNavigationEvent {
    data object Back : UnlockGateNavigationEvent
    data class Unlocked(val targetId: String, val isFolder: Boolean) : UnlockGateNavigationEvent
}
