package com.wissli.app.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [CategoryEntity::class, QuestionEntity::class, ContentInfoEntity::class],
    version = 1,
)
abstract class WissliDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao

    companion object {
        const val NAME = "wissli.db"
    }
}
