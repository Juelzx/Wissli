package com.wissli.app.data.question

import com.wissli.app.core.model.Category
import com.wissli.app.core.model.Question
import kotlinx.coroutines.flow.Flow

/** All texts come in the app language and follow it when it changes. */
interface QuestionRepository {
    fun observeCategories(): Flow<List<Category>>

    /** Ordered by [Question.order]. */
    suspend fun getLevelQuestions(
        categoryId: String,
        level: Int,
    ): List<Question>

    suspend fun getQuestionIds(): List<String>

    /** Emits null while no question with this id exists. */
    fun observeQuestion(questionId: String): Flow<Question?>
}
