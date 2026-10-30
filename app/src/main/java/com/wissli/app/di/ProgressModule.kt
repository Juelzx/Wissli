package com.wissli.app.di

import com.wissli.app.data.progress.FirestoreProgressRemoteDataSource
import com.wissli.app.data.progress.OfflineFirstProgressRepository
import com.wissli.app.data.progress.ProgressRemoteDataSource
import com.wissli.app.data.progress.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val progressModule =
    module {
        single<ProgressRemoteDataSource> { FirestoreProgressRemoteDataSource(firestore = get()) }
        single<ProgressRepository> {
            OfflineFirstProgressRepository(
                authRepository = get(),
                progressDao = get(),
                remoteDataSource = get(),
                // Outlives single screens, so an upload started while leaving a level still finishes.
                syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            )
        }
    }
