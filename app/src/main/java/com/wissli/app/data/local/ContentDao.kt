package com.wissli.app.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

// Room needs one function per statement, and swapping the package alone takes ten of them.
@Suppress("TooManyFunctions")
@Dao
abstract class ContentDao {
    @Query(
        """
        SELECT c.id, n.name, c.sortOrder, COALESCE(MAX(q.level), 0) AS levelCount
        FROM categories c
        JOIN category_names n ON n.categoryId = c.id AND n.language = :language
        LEFT JOIN questions q ON q.categoryId = c.id
        GROUP BY c.id
        ORDER BY c.sortOrder
        """,
    )
    abstract fun observeCategories(language: String): Flow<List<CategoryWithLevelCount>>

    @Query(
        """
        SELECT $LOCALIZED_QUESTION_COLUMNS
        FROM questions q JOIN question_texts t ON t.questionId = q.id AND t.language = :language
        WHERE q.categoryId = :categoryId AND q.level = :level
        ORDER BY q.sortOrder
        """,
    )
    abstract suspend fun getLevelQuestions(
        categoryId: String,
        level: Int,
        language: String,
    ): List<LocalizedQuestion>

    @Query(
        """
        SELECT $LOCALIZED_QUESTION_COLUMNS
        FROM questions q JOIN question_texts t ON t.questionId = q.id AND t.language = :language
        WHERE q.id = :questionId
        """,
    )
    abstract fun observeQuestion(
        questionId: String,
        language: String,
    ): Flow<LocalizedQuestion?>

    @Query("SELECT id FROM questions ORDER BY id")
    abstract suspend fun getQuestionIds(): List<String>

    @Query("SELECT contentVersion FROM content_info WHERE id = ${ContentInfoEntity.SINGLE_ROW_ID}")
    abstract suspend fun getContentVersion(): Int?

    /** Swaps the whole package at once, so readers never see half of an update. */
    @Transaction
    open suspend fun replaceContent(
        rows: ContentRows,
        contentVersion: Int,
    ) {
        deleteQuestionTexts()
        deleteQuestions()
        deleteCategoryNames()
        deleteCategories()
        insertCategories(rows.categories)
        insertCategoryNames(rows.categoryNames)
        insertQuestions(rows.questions)
        insertQuestionTexts(rows.questionTexts)
        upsertContentInfo(ContentInfoEntity(contentVersion = contentVersion))
    }

    @Query("DELETE FROM question_texts")
    abstract suspend fun deleteQuestionTexts()

    @Query("DELETE FROM questions")
    abstract suspend fun deleteQuestions()

    @Query("DELETE FROM category_names")
    abstract suspend fun deleteCategoryNames()

    @Query("DELETE FROM categories")
    abstract suspend fun deleteCategories()

    @Insert
    abstract suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert
    abstract suspend fun insertCategoryNames(names: List<CategoryNameEntity>)

    @Insert
    abstract suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert
    abstract suspend fun insertQuestionTexts(texts: List<QuestionTextEntity>)

    @Upsert
    abstract suspend fun upsertContentInfo(info: ContentInfoEntity)

    private companion object {
        const val LOCALIZED_QUESTION_COLUMNS =
            "q.id, q.categoryId, q.level, q.sortOrder, q.correctAnswerIndex, q.difficulty, " +
                "t.title, t.answersJson, t.explanation"
    }
}
