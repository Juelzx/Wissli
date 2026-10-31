package com.wissli.app.features.profile.presentation

import com.wissli.app.core.model.AppLanguage

data class ProfileState(
    val displayName: String? = null,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val notificationsEnabled: Boolean = true,
)

sealed interface ProfileIntent {
    data class LanguageSelected(
        val language: AppLanguage,
    ) : ProfileIntent

    data class NotificationsToggled(
        val enabled: Boolean,
    ) : ProfileIntent
}
