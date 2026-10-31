package com.wissli.app.data.settings

import com.wissli.app.core.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    /** Also decides the language of the questions. */
    fun observeLanguage(): StateFlow<AppLanguage>

    /** Recreates the running activity so every screen picks up the new language. */
    fun setLanguage(language: AppLanguage)

    fun observeNotificationsEnabled(): Flow<Boolean>

    suspend fun setNotificationsEnabled(enabled: Boolean)
}
