package com.wissli.app.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
)

// Answers are stored as a JSON array string: a converter would add little for a single column.
@Entity(tableName = "questions", indices = [Index(value = ["categoryId", "level", "sortOrder"])])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val level: Int,
    val sortOrder: Int,
    val title: String,
    val answersJson: String,
    val correctAnswerIndex: Int,
    val explanation: String,
    val ageFrom: Int,
    val ageTo: Int,
    val difficulty: String?,
)

/** Single row with the version of the stored question package. */
@Entity(tableName = "content_info")
data class ContentInfoEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val contentVersion: Int,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

data class CategoryWithLevelCount(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val levelCount: Int,
)

data class DailyQuestionRow(
    val id: String,
    val title: String,
    val categoryName: String,
    val ageFrom: Int,
    val ageTo: Int,
)
