package com.wissli.app.di

import com.wissli.app.data.settings.LocalSettingsRepository
import com.wissli.app.data.settings.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val settingsModule =
    module {
        single<SettingsRepository> { LocalSettingsRepository(context = androidContext()) }
    }
