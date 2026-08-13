package com.anacardix.jottiq.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide, in-memory "I already unlocked something this session" flag. Passing the biometric
 * gate ([AppLockManager]) for *any* locked note or folder [unlock]s every locked item in the app
 * — no re-gating needed to browse into another one — until the process backgrounds and [lock]
 * resets it. Never persisted: this is deliberately separate from a note/folder's own
 * [com.anacardix.jottiq.domain.Folder.isLocked] / [com.anacardix.jottiq.domain.Note.isLocked],
 * which only changes via an explicit lock/unlock action.
 */
@Singleton
class LockSession @Inject constructor() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
    }
}
