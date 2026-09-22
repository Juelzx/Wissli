package com.wissli.app.feature.auth.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.auth.GoogleAuthDataSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val googleAuthDataSource: GoogleAuthDataSource,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<LoginEffect>()
    val effect: SharedFlow<LoginEffect> = _effect

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.SignInWithGoogleClicked -> signInWithGoogle(intent.activityContext)
            LoginIntent.ErrorMessageShown -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun signInWithGoogle(activityContext: Context) {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result =
                runCatching {
                    val idToken = googleAuthDataSource.requestGoogleIdToken(activityContext)
                    authRepository.signInWithGoogle(idToken).getOrThrow()
                }

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _effect.emit(LoginEffect.NavigateToHome)
                },
                onFailure = { throwable ->
                    // TODO: throwable-Typen unterscheiden, sobald mehr Fehlerfälle relevant werden —
                    // z. B. GetCredentialCancellationException (Nutzer hat den Picker weggetippt)
                    // sollte still abgebrochen werden statt eine Fehlermeldung zu zeigen.
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Anmeldung fehlgeschlagen. Bitte versuche es erneut.",
                        )
                    }
                },
            )
        }
    }
}
