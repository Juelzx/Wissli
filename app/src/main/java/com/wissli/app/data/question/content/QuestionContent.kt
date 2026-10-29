package com.wissli.app.data.question.content

import com.wissli.app.core.model.Difficulty

/**
 * A complete question package as shipped in the app's assets and, later, as downloaded update.
 * A package always replaces the stored content as a whole; [contentVersion] decides whether it
 * is newer than what is stored.
 */
data class QuestionContent(
    val contentVersion: Int,
    val categories: List<ContentCategory>,
    val questions: List<ContentQuestion>,
)

data class ContentCategory(
    val id: String,
    val name: String,
    val order: Int,
)

data class ContentQuestion(
    val id: String,
    val categoryId: String,
    val level: Int,
    val order: Int,
    val title: String,
    val answers: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val ageFrom: Int,
    val ageTo: Int,
    val difficulty: Difficulty?,
)
