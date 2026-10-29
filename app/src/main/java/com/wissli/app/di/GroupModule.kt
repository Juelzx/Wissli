package com.wissli.app.di

import com.wissli.app.data.group.GroupRepository
import com.wissli.app.data.group.PlaceholderGroupRepository
import org.koin.dsl.module

val groupModule =
    module {
        single<GroupRepository> { PlaceholderGroupRepository() }
    }
