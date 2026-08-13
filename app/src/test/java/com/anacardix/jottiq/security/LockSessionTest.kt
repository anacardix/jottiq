package com.anacardix.jottiq.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LockSessionTest {

    @Test
    fun `starts locked`() {
        assertThat(LockSession().isUnlocked.value).isFalse()
    }

    @Test
    fun `unlock flips the session to unlocked`() {
        val lockSession = LockSession()

        lockSession.unlock()

        assertThat(lockSession.isUnlocked.value).isTrue()
    }

    @Test
    fun `lock resets an unlocked session`() {
        val lockSession = LockSession()
        lockSession.unlock()

        lockSession.lock()

        assertThat(lockSession.isUnlocked.value).isFalse()
    }
}
