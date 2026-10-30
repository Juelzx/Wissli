package com.wissli.app.data.local

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [CategoryEntity::class, QuestionEntity::class, ContentInfoEntity::class, ProgressEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class WissliDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao

    abstract fun progressDao(): ProgressDao

    companion object {
        const val NAME = "wissli.db"
    }
}
