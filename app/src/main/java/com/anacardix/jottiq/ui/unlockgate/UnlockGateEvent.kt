package com.anacardix.jottiq.ui.unlockgate

import androidx.fragment.app.FragmentActivity

/** Intents for [UnlockGateScreen]. */
sealed interface UnlockGateEvent {
    data object BackClicked : UnlockGateEvent

    /** [promptTitle] is a resolved string resource — [UnlockGateViewModel] can't call
     * `stringResource` itself, so the screen builds it and passes it through. */
    data class UnlockClicked(
        val activity: FragmentActivity,
        val promptTitle: String,
    ) : UnlockGateEvent

    data object UserMessageShown : UnlockGateEvent
}
