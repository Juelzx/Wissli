package com.wissli.app.di

import com.wissli.app.data.question.QuestionRepository
import com.wissli.app.data.question.RoomQuestionRepository
import com.wissli.app.data.question.content.AssetBundledContentSource
import com.wissli.app.data.question.content.BundledContentSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val questionModule =
    module {
        single<BundledContentSource> { AssetBundledContentSource(context = androidContext()) }
        single<QuestionRepository> { RoomQuestionRepository(contentDao = get(), bundledContentSource = get()) }
    }
