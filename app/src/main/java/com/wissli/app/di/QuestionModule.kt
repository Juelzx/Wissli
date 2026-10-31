package com.wissli.app.di

import com.wissli.app.data.daily.DailyQuestionRepository
import com.wissli.app.data.daily.DailyQuestionStore
import com.wissli.app.data.daily.LocalDailyQuestionRepository
import com.wissli.app.data.daily.SharedPreferencesDailyQuestionStore
import com.wissli.app.data.question.QuestionRepository
import com.wissli.app.data.question.RoomQuestionRepository
import com.wissli.app.data.question.content.AssetBundledContentSource
import com.wissli.app.data.question.content.BundledContentSource
import com.wissli.app.data.settings.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val questionModule =
    module {
        single<BundledContentSource> { AssetBundledContentSource(context = androidContext()) }
        single<QuestionRepository> {
            RoomQuestionRepository(
                contentDao = get(),
                bundledContentSource = get(),
                language = get<SettingsRepository>().observeLanguage(),
            )
        }
        single<DailyQuestionStore> { SharedPreferencesDailyQuestionStore(context = androidContext()) }
        single<DailyQuestionRepository> { LocalDailyQuestionRepository(questionRepository = get(), store = get()) }
    }
