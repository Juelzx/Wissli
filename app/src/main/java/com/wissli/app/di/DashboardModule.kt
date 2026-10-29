package com.wissli.app.di

import com.wissli.app.features.dashboard.presentation.DashboardViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dashboardModule =
    module {
        viewModelOf(::DashboardViewModel)
    }
