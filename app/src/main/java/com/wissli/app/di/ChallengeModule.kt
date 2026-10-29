package com.wissli.app.di

import com.wissli.app.data.challenge.ChallengeRepository
import com.wissli.app.data.challenge.PlaceholderChallengeRepository
import org.koin.dsl.module

val challengeModule =
    module {
        single<ChallengeRepository> { PlaceholderChallengeRepository() }
    }
