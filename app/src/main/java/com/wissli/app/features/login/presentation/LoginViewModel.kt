package com.wissli.app.features.login.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.auth.GoogleAccountSelection
import com.wissli.app.data.auth.GoogleAuthDataSource
import com.wissli.app.data.auth.GoogleSignInCancelledException
import com.wissli.app.data.auth.NoGoogleAccountException
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

    init {
        viewModelScope.launch {
            val lastAccount = authRepository.getLastSignedInAccount()
            _state.update { it.copy(isLoadingLastAccount = false, lastAccount = lastAccount) }
        }
    }

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.ContinueWithGoogleClicked ->
                signInWithGoogle(intent.activityContext, GoogleAccountSelection.PREFER_AUTHORIZED)

            is LoginIntent.UseAnotherAccountClicked ->
                signInWithGoogle(intent.activityContext, GoogleAccountSelection.ANY_ACCOUNT)

            is LoginIntent.ErrorShown -> _state.update { it.copy(error = null) }
        }
    }

    private fun signInWithGoogle(
        activityContext: Context,
        selection: GoogleAccountSelection,
    ) {
        if (_state.value.isSigningIn) return

        viewModelScope.launch {
            _state.update { it.copy(isSigningIn = true, error = null) }

            val result =
                runCatching {
                    val idToken = googleAuthDataSource.requestGoogleIdToken(activityContext, selection)
                    authRepository.signInWithGoogle(idToken).getOrThrow()
                }

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isSigningIn = false) }
                    _effect.emit(LoginEffect.NavigateToHome)
                },
                onFailure = { throwable ->
                    Log.d(TAG, throwable.toString())
                    _state.update { it.copy(isSigningIn = false, error = throwable.toLoginErrorOrNull()) }
                },
            )
        }
    }

    // A cancelled picker is not an error, so no message is shown.
    private fun Throwable.toLoginErrorOrNull(): LoginError? =
        when (this) {
            is GoogleSignInCancelledException -> null
            is NoGoogleAccountException -> LoginError.NO_GOOGLE_ACCOUNT
            else -> LoginError.SIGN_IN_FAILED
        }

    private companion object {
        const val TAG = "LoginViewModel"
    }
}
