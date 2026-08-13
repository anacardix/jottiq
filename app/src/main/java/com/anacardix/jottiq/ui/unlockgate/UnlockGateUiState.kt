package com.anacardix.jottiq.ui.unlockgate

import com.anacardix.jottiq.ui.common.UserMessage

/** UI state for [UnlockGateScreen], modeled on `design/16. Locked notes.png`. */
data class UnlockGateUiState(
    val targetId: String = "",
    val targetName: String = "",
    val isFolder: Boolean = false,
    val isAuthenticating: Boolean = false,
    val userMessage: UserMessage? = null,
)
