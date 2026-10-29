package com.wissli.app.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ContentDao {
    @Query(
        """
        SELECT c.id, c.name, c.sortOrder, COALESCE(MAX(q.level), 0) AS levelCount
        FROM categories c LEFT JOIN questions q ON q.categoryId = c.id
        GROUP BY c.id
        ORDER BY c.sortOrder
        """,
    )
    abstract fun observeCategories(): Flow<List<CategoryWithLevelCount>>

    @Query("SELECT * FROM questions WHERE categoryId = :categoryId AND level = :level ORDER BY sortOrder")
    abstract suspend fun getLevelQuestions(
        categoryId: String,
        level: Int,
    ): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions")
    abstract suspend fun countQuestions(): Int

    @Query(
        """
        SELECT q.id, q.title, c.name AS categoryName, q.ageFrom, q.ageTo
        FROM questions q JOIN categories c ON c.id = q.categoryId
        ORDER BY q.id LIMIT 1 OFFSET :offset
        """,
    )
    abstract suspend fun getDailyQuestionAt(offset: Int): DailyQuestionRow?

    @Query("SELECT contentVersion FROM content_info WHERE id = ${ContentInfoEntity.SINGLE_ROW_ID}")
    abstract suspend fun getContentVersion(): Int?

    /** Swaps the whole package at once, so readers never see half of an update. */
    @Transaction
    open suspend fun replaceContent(
        categories: List<CategoryEntity>,
        questions: List<QuestionEntity>,
        contentVersion: Int,
    ) {
        deleteQuestions()
        deleteCategories()
        insertCategories(categories)
        insertQuestions(questions)
        upsertContentInfo(ContentInfoEntity(contentVersion = contentVersion))
    }

    @Query("DELETE FROM questions")
    abstract suspend fun deleteQuestions()

    @Query("DELETE FROM categories")
    abstract suspend fun deleteCategories()

    @Insert
    abstract suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert
    abstract suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Upsert
    abstract suspend fun upsertContentInfo(info: ContentInfoEntity)
}
