package com.wissli.app.data.question

import com.wissli.app.core.model.Category
import com.wissli.app.core.model.Question
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun observeDailyQuestion(): Flow<DailyQuestion?>

    fun observeCategories(): Flow<List<Category>>

    /** Ordered by [Question.order]. */
    suspend fun getLevelQuestions(
        categoryId: String,
        level: Int,
    ): List<Question>
}

data class DailyQuestion(
    val questionId: String,
    val title: String,
    val categoryName: String,
    val ageFrom: Int,
    val ageTo: Int,
)
