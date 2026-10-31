package com.wissli.app.data.settings

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.wissli.app.core.model.AppLanguage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * The language is stored by AppCompat's per-app language support (on Android 13+ by the system,
 * below that through the autoStoreLocales service in the manifest), so it also shows up in the
 * system's app language settings.
 */
class LocalSettingsRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SettingsRepository {
    private val appContext = context.applicationContext
    private val prefs by lazy { appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val language = MutableStateFlow(currentLanguage())
    private val notificationsEnabled = MutableStateFlow<Boolean?>(null)

    init {
        // Follows the system language while the user has not picked one in the app.
        appContext.registerComponentCallbacks(
            object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    language.value = currentLanguage()
                }

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onLowMemory() = Unit
            },
        )
    }

    override fun observeLanguage(): StateFlow<AppLanguage> = language.asStateFlow()

    override fun setLanguage(language: AppLanguage) {
        if (language == this.language.value) return
        this.language.value = language
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    override fun observeNotificationsEnabled(): Flow<Boolean> =
        flow {
            if (notificationsEnabled.value == null) {
                notificationsEnabled.compareAndSet(null, withContext(ioDispatcher) { readNotificationsEnabled() })
            }
            notificationsEnabled.collect { enabled -> if (enabled != null) emit(enabled) }
        }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled.value = enabled
        withContext(ioDispatcher) { prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) } }
    }

    private fun readNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    private fun currentLanguage(): AppLanguage {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val locale = if (appLocales.isEmpty) LocaleListCompat.getAdjustedDefault()[0] else appLocales[0]
        return AppLanguage.fromTag(locale?.language)
    }

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }
}
