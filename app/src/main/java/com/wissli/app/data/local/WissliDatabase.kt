package com.wissli.app.data.local

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.DeleteColumn
import androidx.room3.RoomDatabase
import androidx.room3.migration.AutoMigrationSpec

@Database(
    entities = [
        CategoryEntity::class,
        CategoryNameEntity::class,
        QuestionEntity::class,
        QuestionTextEntity::class,
        ContentInfoEntity::class,
        ProgressEntity::class,
        AnswerEntity::class,
    ],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = LocalizedContentMigration::class),
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class WissliDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao

    abstract fun progressDao(): ProgressDao

    abstract fun answerDao(): AnswerDao

    companion object {
        const val NAME = "wissli.db"
    }
}

/**
 * Texts moved into per-language tables. The rows left behind have no texts until the next
 * package is stored, which happens before the first read because its contentVersion is higher.
 */
@DeleteColumn.Entries(
    DeleteColumn(tableName = "categories", columnName = "name"),
    DeleteColumn(tableName = "questions", columnName = "title"),
    DeleteColumn(tableName = "questions", columnName = "answersJson"),
    DeleteColumn(tableName = "questions", columnName = "explanation"),
)
class LocalizedContentMigration : AutoMigrationSpec
