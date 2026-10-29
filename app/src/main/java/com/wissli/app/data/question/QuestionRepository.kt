package com.wissli.app.data.question

import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun observeDailyQuestion(): Flow<DailyQuestion?>
}

data class DailyQuestion(
    val questionId: String,
    val title: String,
    val categoryName: String,
    val ageFrom: Int,
    val ageTo: Int,
)
