package com.wissli.app.data.question

import com.wissli.app.core.model.Category
import com.wissli.app.core.model.Difficulty
import com.wissli.app.core.model.Question
import com.wissli.app.data.local.CategoryEntity
import com.wissli.app.data.local.CategoryNameEntity
import com.wissli.app.data.local.CategoryWithLevelCount
import com.wissli.app.data.local.ContentRows
import com.wissli.app.data.local.LocalizedQuestion
import com.wissli.app.data.local.QuestionEntity
import com.wissli.app.data.local.QuestionTextEntity
import com.wissli.app.data.question.content.QuestionContent
import org.json.JSONArray

internal fun QuestionContent.toRows() =
    ContentRows(
        categories = categories.map { CategoryEntity(id = it.id, sortOrder = it.order) },
        categoryNames =
            categories.flatMap { category ->
                category.names.map { (language, name) -> CategoryNameEntity(category.id, language, name) }
            },
        questions =
            questions.map {
                QuestionEntity(
                    id = it.id,
                    categoryId = it.categoryId,
                    level = it.level,
                    sortOrder = it.order,
                    correctAnswerIndex = it.correctAnswerIndex,
                    ageFrom = it.ageFrom,
                    ageTo = it.ageTo,
                    difficulty = it.difficulty?.name,
                )
            },
        questionTexts =
            questions.flatMap { question ->
                question.texts.map { (language, text) ->
                    QuestionTextEntity(
                        questionId = question.id,
                        language = language,
                        title = text.title,
                        answersJson = JSONArray(text.answers).toString(),
                        explanation = text.explanation,
                    )
                }
            },
    )

internal fun CategoryWithLevelCount.toModel() =
    Category(id = id, name = name, order = sortOrder, levelCount = levelCount)

internal fun LocalizedQuestion.toModel(): Question {
    val answers = JSONArray(answersJson)
    return Question(
        id = id,
        categoryId = categoryId,
        level = level,
        order = sortOrder,
        title = title,
        answers = (0 until answers.length()).map(answers::getString),
        correctAnswerIndex = correctAnswerIndex,
        explanation = explanation,
        difficulty = difficulty?.let(Difficulty::valueOf),
    )
}
