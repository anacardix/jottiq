package com.anacardix.jottiq.security

import androidx.fragment.app.FragmentActivity

/** Outcome of an [AppLockManager.authenticate] call. */
sealed interface AppLockResult {
    data object Success : AppLockResult
    data object Cancelled : AppLockResult
    data class Failed(val message: String?) : AppLockResult
}

/**
 * Gate for viewing/editing a locked note or folder. Implemented against `BiometricPrompt` in
 * `security/BiometricAppLockManager` — callers (ViewModels) never touch BiometricPrompt directly.
 *
 * [activity] is taken as a call-time parameter, not stored, so implementations don't hold an
 * Activity reference beyond the single suspending call.
 */
interface AppLockManager {
    suspend fun authenticate(activity: FragmentActivity, title: String, subtitle: String): AppLockResult
}
