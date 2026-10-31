package com.wissli.app.di

import org.koin.dsl.module

val appModule =
    module {
        includes(
            authModule,
            databaseModule,
            settingsModule,
            groupModule,
            questionModule,
            progressModule,
            statsModule,
            dashboardModule,
            learnModule,
            profileModule,
        )
    }
