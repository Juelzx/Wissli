package com.wissli.app.features.login.presentation

import android.content.Context

data class LoginState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface LoginIntent {
    data class SignInWithGoogleClicked(
        val activityContext: Context,
    ) : LoginIntent

    data object ErrorMessageShown : LoginIntent
}

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
}
