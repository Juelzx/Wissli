package com.wissli.app.di

import com.wissli.app.data.stats.LocalStatsRepository
import com.wissli.app.data.stats.StatsRepository
import org.koin.dsl.module

val statsModule =
    module {
        single<StatsRepository> {
            LocalStatsRepository(
                authRepository = get(),
                answerDao = get(),
                questionRepository = get(),
                progressRepository = get(),
            )
        }
    }
