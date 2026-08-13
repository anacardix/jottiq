package com.anacardix.jottiq.ui.unlockgate

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anacardix.jottiq.R
import com.anacardix.jottiq.security.AppLockManager
import com.anacardix.jottiq.security.AppLockResult
import com.anacardix.jottiq.security.LockSession
import com.anacardix.jottiq.ui.common.UserMessage
import com.anacardix.jottiq.ui.navigation.UnlockGateRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnlockGateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appLockManager: AppLockManager,
    private val lockSession: LockSession,
) : ViewModel() {

    private val route: UnlockGateRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(
        UnlockGateUiState(targetId = route.targetId, targetName = route.targetName, isFolder = route.isFolder),
    )
    val uiState: StateFlow<UnlockGateUiState> = _uiState.asStateFlow()

    private val navigationChannel = Channel<UnlockGateNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<UnlockGateNavigationEvent> = navigationChannel.receiveAsFlow()

    fun onEvent(event: UnlockGateEvent) {
        when (event) {
            UnlockGateEvent.BackClicked -> navigationChannel.trySend(UnlockGateNavigationEvent.Back)
            is UnlockGateEvent.UnlockClicked -> onUnlockClicked(event.activity, event.promptTitle)
            UnlockGateEvent.UserMessageShown -> _uiState.update { it.copy(userMessage = null) }
        }
    }

    private fun onUnlockClicked(activity: FragmentActivity, promptTitle: String) {
        _uiState.update { it.copy(isAuthenticating = true) }
        viewModelScope.launch {
            when (appLockManager.authenticate(activity, promptTitle, subtitle = "")) {
                AppLockResult.Success -> {
                    lockSession.unlock()
                    _uiState.update { it.copy(isAuthenticating = false) }
                    val state = _uiState.value
                    navigationChannel.trySend(UnlockGateNavigationEvent.Unlocked(state.targetId, state.isFolder))
                }
                AppLockResult.Cancelled -> _uiState.update { it.copy(isAuthenticating = false) }
                is AppLockResult.Failed -> _uiState.update {
                    it.copy(isAuthenticating = false, userMessage = UserMessage(R.string.unlock_gate_error))
                }
            }
        }
    }
}
