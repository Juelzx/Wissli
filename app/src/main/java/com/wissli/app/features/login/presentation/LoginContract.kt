package com.wissli.app.features.login.presentation

import android.content.Context
import com.wissli.app.data.auth.LastSignedInAccount

data class LoginState(
    val isLoadingLastAccount: Boolean = true,
    val lastAccount: LastSignedInAccount? = null,
    val isSigningIn: Boolean = false,
    val error: LoginError? = null,
) {
    val greetingName: String?
        get() =
            lastAccount
                ?.displayName
                ?.trim()
                ?.substringBefore(' ')
                ?.takeIf { it.isNotBlank() }
}

enum class LoginError {
    NO_GOOGLE_ACCOUNT,
    SIGN_IN_FAILED,
}

sealed interface LoginIntent {
    data class ContinueWithGoogleClicked(
        val activityContext: Context,
    ) : LoginIntent

    data class UseAnotherAccountClicked(
        val activityContext: Context,
    ) : LoginIntent

    data object ErrorShown : LoginIntent
}

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
}
