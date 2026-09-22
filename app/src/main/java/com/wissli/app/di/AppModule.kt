package com.wissli.app.di

import org.koin.dsl.module

/** Wurzel-Modul: hängt die Feature-Module ein. Neue Feature-Module hier per includes() ergänzen. */
val appModule = module {
    includes(authModule)
}
