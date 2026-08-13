package com.anacardix.jottiq.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe route for [com.anacardix.jottiq.ui.unlockgate.UnlockGateScreen], shown instead of
 * navigating straight to a locked folder or note (`design/16. Locked notes.png`). [targetName] is
 * passed through directly from the calling screen's already-loaded row state, so the gate needs
 * no repository lookup of its own.
 */
@Serializable
data class UnlockGateRoute(val targetId: String, val targetName: String, val isFolder: Boolean)
