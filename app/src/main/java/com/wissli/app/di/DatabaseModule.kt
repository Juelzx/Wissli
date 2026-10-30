package com.wissli.app.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.wissli.app.data.local.ContentDao
import com.wissli.app.data.local.ProgressDao
import com.wissli.app.data.local.WissliDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule =
    module {
        single<WissliDatabase> {
            Room
                .databaseBuilder<WissliDatabase>(androidContext(), WissliDatabase.NAME)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        single<ContentDao> { get<WissliDatabase>().contentDao() }
        single<ProgressDao> { get<WissliDatabase>().progressDao() }
    }
