package com.anacardix.jottiq.ui.unlockgate

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.anacardix.jottiq.MainDispatcherRule
import com.anacardix.jottiq.R
import com.anacardix.jottiq.fakes.FakeAppLockManager
import com.anacardix.jottiq.security.AppLockResult
import com.anacardix.jottiq.security.LockSession
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UnlockGateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appLockManager = FakeAppLockManager()
    private val lockSession = LockSession()
    private val activity: FragmentActivity = Robolectric.buildActivity(FragmentActivity::class.java).get()

    private fun viewModel(
        targetId: String = "note-1",
        targetName: String = "Gift ideas",
        isFolder: Boolean = false,
    ) = UnlockGateViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf("targetId" to targetId, "targetName" to targetName, "isFolder" to isFolder),
        ),
        appLockManager = appLockManager,
        lockSession = lockSession,
    )

    @Test
    fun `initial state carries the target from the route`() {
        val state = viewModel(targetId = "note-1", targetName = "Gift ideas", isFolder = false).uiState.value

        assertThat(state.targetId).isEqualTo("note-1")
        assertThat(state.targetName).isEqualTo("Gift ideas")
        assertThat(state.isFolder).isFalse()
    }

    @Test
    fun `successful auth navigates to Unlocked with the target`() = runTest {
        appLockManager.nextResult = AppLockResult.Success
        val viewModel = viewModel(targetId = "note-1", isFolder = false)

        viewModel.navigationEvents.test {
            viewModel.onEvent(UnlockGateEvent.UnlockClicked(activity, "Unlock"))

            assertThat(awaitItem()).isEqualTo(UnlockGateNavigationEvent.Unlocked("note-1", false))
        }
        assertThat(viewModel.uiState.value.isAuthenticating).isFalse()
    }

    @Test
    fun `successful auth unlocks the app-wide session`() = runTest {
        appLockManager.nextResult = AppLockResult.Success
        val viewModel = viewModel()

        viewModel.onEvent(UnlockGateEvent.UnlockClicked(activity, "Unlock"))

        assertThat(lockSession.isUnlocked.value).isTrue()
    }

    @Test
    fun `cancelled auth stays on the gate without a message`() = runTest {
        appLockManager.nextResult = AppLockResult.Cancelled
        val viewModel = viewModel()

        viewModel.onEvent(UnlockGateEvent.UnlockClicked(activity, "Unlock"))

        assertThat(viewModel.uiState.value.isAuthenticating).isFalse()
        assertThat(viewModel.uiState.value.userMessage).isNull()
        assertThat(lockSession.isUnlocked.value).isFalse()
    }

    @Test
    fun `failed auth surfaces a user message`() = runTest {
        appLockManager.nextResult = AppLockResult.Failed("hardware error")
        val viewModel = viewModel()

        viewModel.onEvent(UnlockGateEvent.UnlockClicked(activity, "Unlock"))

        assertThat(viewModel.uiState.value.userMessage?.messageResId).isEqualTo(R.string.unlock_gate_error)
    }

    @Test
    fun `authenticate is called with the resolved prompt title and no subtitle`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(UnlockGateEvent.UnlockClicked(activity, "Unlock \"Gift ideas\""))

        assertThat(appLockManager.lastTitle).isEqualTo("Unlock \"Gift ideas\"")
        assertThat(appLockManager.lastSubtitle).isEmpty()
    }

    @Test
    fun `BackClicked emits the Back navigation event`() = runTest {
        val viewModel = viewModel()

        viewModel.navigationEvents.test {
            viewModel.onEvent(UnlockGateEvent.BackClicked)

            assertThat(awaitItem()).isEqualTo(UnlockGateNavigationEvent.Back)
        }
    }

    @Test
    fun `UserMessageShown clears the pending message`() = runTest {
        appLockManager.nextResult = AppLockResult.Failed(null)
        val viewModel = viewModel()
        viewModel.onEvent(UnlockGateEvent.UnlockClicked(activity, "Unlock"))
        assertThat(viewModel.uiState.value.userMessage).isNotNull()

        viewModel.onEvent(UnlockGateEvent.UserMessageShown)

        assertThat(viewModel.uiState.value.userMessage).isNull()
    }
}
