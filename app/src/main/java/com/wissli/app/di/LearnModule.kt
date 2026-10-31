package com.wissli.app.di

import com.wissli.app.features.category.presentation.CategoryViewModel
import com.wissli.app.features.learn.presentation.LearnViewModel
import com.wissli.app.features.level.presentation.LevelViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val learnModule =
    module {
        viewModelOf(::LearnViewModel)
        viewModel { (categoryId: String) -> CategoryViewModel(categoryId, get(), get()) }
        viewModel { (categoryId: String, level: Int) -> LevelViewModel(categoryId, level, get(), get(), get()) }
    }
