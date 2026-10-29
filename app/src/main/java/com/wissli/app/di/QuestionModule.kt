package com.wissli.app.di

import com.wissli.app.data.question.PlaceholderQuestionRepository
import com.wissli.app.data.question.QuestionRepository
import org.koin.dsl.module

val questionModule =
    module {
        single<QuestionRepository> { PlaceholderQuestionRepository() }
    }
