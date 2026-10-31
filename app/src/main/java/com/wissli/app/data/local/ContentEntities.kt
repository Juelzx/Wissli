package com.wissli.app.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
)

@Entity(tableName = "category_names", primaryKeys = ["categoryId", "language"])
data class CategoryNameEntity(
    val categoryId: String,
    val language: String,
    val name: String,
)

@Entity(tableName = "questions", indices = [Index(value = ["categoryId", "level", "sortOrder"])])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val level: Int,
    val sortOrder: Int,
    val correctAnswerIndex: Int,
    val ageFrom: Int,
    val ageTo: Int,
    val difficulty: String?,
)

// Answers are stored as a JSON array string: a converter would add little for a single column.
@Entity(tableName = "question_texts", primaryKeys = ["questionId", "language"])
data class QuestionTextEntity(
    val questionId: String,
    val language: String,
    val title: String,
    val answersJson: String,
    val explanation: String,
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

/** A question joined with its texts in one language. */
data class LocalizedQuestion(
    val id: String,
    val categoryId: String,
    val level: Int,
    val sortOrder: Int,
    val correctAnswerIndex: Int,
    val difficulty: String?,
    val title: String,
    val answersJson: String,
    val explanation: String,
)

/** Everything [ContentDao.replaceContent] swaps in one transaction. */
data class ContentRows(
    val categories: List<CategoryEntity>,
    val categoryNames: List<CategoryNameEntity>,
    val questions: List<QuestionEntity>,
    val questionTexts: List<QuestionTextEntity>,
)
