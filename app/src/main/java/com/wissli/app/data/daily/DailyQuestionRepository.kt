package com.wissli.app.data.daily

import com.wissli.app.core.model.Question
import kotlinx.coroutines.flow.Flow

/**
 * One question per local calendar day, picked at random from all questions. It stays the same
 * until midnight, also once answered, and never repeats the previous day's question.
 */
interface DailyQuestionRepository {
    /** Switches to the next question at local midnight while collected. */
    fun observeDailyQuestion(): Flow<DailyQuestion?>

    /** Only the first answer of the day counts; later calls change nothing. */
    suspend fun answer(
        questionId: String,
        answerIndex: Int,
    )
}

/** The daily question is separate from the solo path: answering it never changes progress. */
data class DailyQuestion(
    val question: Question,
    val categoryName: String,
    val answeredIndex: Int?,
) {
    val isAnswered: Boolean
        get() = answeredIndex != null

    val isAnsweredCorrectly: Boolean
        get() = answeredIndex == question.correctAnswerIndex
}
