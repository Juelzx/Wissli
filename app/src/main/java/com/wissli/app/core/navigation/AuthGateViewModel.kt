package com.wissli.app.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.core.model.User
import com.wissli.app.data.auth.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

sealed interface AuthGateState {
    data object Loading : AuthGateState

    data object LoggedOut : AuthGateState

    data class LoggedIn(
        val user: User,
    ) : AuthGateState
}

/**
 * Picks the start destination from the sign-in state at app start. Only the first value counts:
 * later sign-in/sign-out changes navigate explicitly, otherwise [WissliNavHost] would rebuild the
 * whole graph and create Home or Login twice.
 */
class AuthGateViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val state: StateFlow<AuthGateState> =
        authRepository
            .observeCurrentUser()
            .take(1)
            .map { user -> if (user != null) AuthGateState.LoggedIn(user) else AuthGateState.LoggedOut }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = AuthGateState.Loading,
            )

    // TODO: move to the profile screen once it exists.
    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
