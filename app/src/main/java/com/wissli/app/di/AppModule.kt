package com.wissli.app.di

import org.koin.dsl.module

val appModule =
    module {
        includes(authModule, databaseModule, groupModule, questionModule, progressModule, dashboardModule, learnModule)
    }
