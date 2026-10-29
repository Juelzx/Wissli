package com.wissli.app.di

import com.wissli.app.data.progress.PlaceholderProgressRepository
import com.wissli.app.data.progress.ProgressRepository
import org.koin.dsl.module

val progressModule =
    module {
        single<ProgressRepository> { PlaceholderProgressRepository() }
    }
