package com.wissli.app.data.question

import com.wissli.app.core.model.Category
import com.wissli.app.core.model.Difficulty
import com.wissli.app.core.model.Question
import com.wissli.app.data.local.CategoryEntity
import com.wissli.app.data.local.CategoryWithLevelCount
import com.wissli.app.data.local.QuestionEntity
import com.wissli.app.data.question.content.ContentCategory
import com.wissli.app.data.question.content.ContentQuestion
import org.json.JSONArray

internal fun ContentCategory.toEntity() = CategoryEntity(id = id, name = name, sortOrder = order)

internal fun ContentQuestion.toEntity() =
    QuestionEntity(
        id = id,
        categoryId = categoryId,
        level = level,
        sortOrder = order,
        title = title,
        answersJson = JSONArray(answers).toString(),
        correctAnswerIndex = correctAnswerIndex,
        explanation = explanation,
        ageFrom = ageFrom,
        ageTo = ageTo,
        difficulty = difficulty?.name,
    )

internal fun CategoryWithLevelCount.toModel() =
    Category(id = id, name = name, order = sortOrder, levelCount = levelCount)

internal fun QuestionEntity.toModel(): Question {
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
