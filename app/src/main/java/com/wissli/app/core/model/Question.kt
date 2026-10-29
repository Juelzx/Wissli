package com.wissli.app.core.model

/**
 * Questions are global content. [level] and [order] place a question on its category's learning
 * path; [correctAnswerIndex] points into [answers]. [id] must never be reused for a different
 * question, because solo progress stores solved question ids.
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
    val difficulty: Difficulty? = null,
)

const val QUESTIONS_PER_LEVEL = 20
