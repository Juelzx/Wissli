package com.wissli.app.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.core.model.User
import com.wissli.app.data.auth.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Ob und als wer die aktuelle Person beim App-Start bereits angemeldet ist. */
sealed interface AuthGateState {
    data object Loading : AuthGateState

    data object LoggedOut : AuthGateState

    data class LoggedIn(
        val user: User,
    ) : AuthGateState
}

/**
 * Entscheidet die Start-Destination des Root-NavHost anhand des aktuellen Login-Status, damit
 * bereits angemeldete Nutzer:innen beim App-Start nicht erneut den Login-Screen sehen. Kapselt
 * [AuthRepository.observeCurrentUser] als root-weiten State statt dass jeder Screen selbst
 * beobachtet.
 */
class AuthGateViewModel(
    authRepository: AuthRepository,
) : ViewModel() {
    val state: StateFlow<AuthGateState> =
        authRepository
            .observeCurrentUser()
            .map { user -> if (user != null) AuthGateState.LoggedIn(user) else AuthGateState.LoggedOut }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AuthGateState.Loading,
            )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
