package com.wissli.app.core.model

/**
 * Questions are global content. [level] and [order] place a question on its category's learning
 * path; [correctAnswerIndex] points into [answers].
 */
data class Question(
    val id: String,
    val categoryId: String,
    val level: Int,
    val order: Int,
    val title: String,
    val answers: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
)

const val QUESTIONS_PER_LEVEL = 20
