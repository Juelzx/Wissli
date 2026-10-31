package com.wissli.app.data.question.content

import com.wissli.app.core.model.Difficulty

/**
 * A complete question package as shipped in the app's assets and, later, as downloaded update.
 * A package always replaces the stored content as a whole; [contentVersion] decides whether it
 * is newer than what is stored. Every text exists in each of [languages] (language tags such as
 * "de"), so switching the app language never leaves a question untranslated.
 */
data class QuestionContent(
    val contentVersion: Int,
    val languages: List<String>,
    val categories: List<ContentCategory>,
    val questions: List<ContentQuestion>,
)

/** [names] maps a language tag to the category name in that language. */
data class ContentCategory(
    val id: String,
    val names: Map<String, String>,
    val order: Int,
)

/** [texts] maps a language tag to the question in that language; all share [correctAnswerIndex]. */
data class ContentQuestion(
    val id: String,
    val categoryId: String,
    val level: Int,
    val order: Int,
    val correctAnswerIndex: Int,
    val ageFrom: Int,
    val ageTo: Int,
    val difficulty: Difficulty?,
    val texts: Map<String, ContentQuestionText>,
)

data class ContentQuestionText(
    val title: String,
    val answers: List<String>,
    val explanation: String,
)
