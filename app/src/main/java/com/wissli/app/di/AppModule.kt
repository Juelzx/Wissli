package com.wissli.app.di

import org.koin.dsl.module

val appModule =
    module {
        includes(authModule, groupModule, questionModule, challengeModule, dashboardModule)
    }
