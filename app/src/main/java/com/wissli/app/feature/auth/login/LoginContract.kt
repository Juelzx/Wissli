package com.wissli.app.feature.auth.login

import android.content.Context

/** Immutable UI-State (siehe CLAUDE.md "MVI-orientiert"). */
data class LoginState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/** Intents/Events, die der Screen an den ViewModel schickt. */
sealed interface LoginIntent {
    /**
     * [activityContext] wird bewusst hier statt im ViewModel-Konstruktor injiziert — Credential
     * Manager braucht einen Activity-Context für den System-Picker, und der ViewModel soll keine
     * Activity-Referenz über den Klick hinaus festhalten (kein Context-Leak).
     */
    data class SignInWithGoogleClicked(
        val activityContext: Context,
    ) : LoginIntent

    data object ErrorMessageShown : LoginIntent
}

/** Einmalige Seiteneffekte (Navigation), die nicht Teil des rekonstruierbaren States sein sollen. */
sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
}
