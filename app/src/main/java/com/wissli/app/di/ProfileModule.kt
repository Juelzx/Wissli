package com.wissli.app.di

import com.wissli.app.features.profile.presentation.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule =
    module {
        viewModelOf(::ProfileViewModel)
    }
