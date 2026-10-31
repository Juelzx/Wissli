package com.wissli.app.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Sign-out is not handled here: like on the dashboard, the navigation graph owns it. */
class ProfileViewModel(
    authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val displayName =
        authRepository
            .observeCurrentUser()
            .map { it?.displayName }
            .onStart { emit(null) }
            .catch { emit(null) }

    val state: StateFlow<ProfileState> =
        combine(
            displayName,
            settingsRepository.observeLanguage(),
            settingsRepository.observeNotificationsEnabled(),
        ) { name, language, notificationsEnabled ->
            ProfileState(displayName = name, language = language, notificationsEnabled = notificationsEnabled)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ProfileState(language = settingsRepository.observeLanguage().value),
        )

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LanguageSelected -> settingsRepository.setLanguage(intent.language)
            // TODO: register for push notifications once groups run on Firestore.
            is ProfileIntent.NotificationsToggled ->
                viewModelScope.launch { settingsRepository.setNotificationsEnabled(intent.enabled) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
