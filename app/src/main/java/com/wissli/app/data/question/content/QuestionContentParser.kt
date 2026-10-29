package com.wissli.app.data.question.content

import com.wissli.app.core.model.Difficulty
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the JSON format of a question package (see app/src/main/assets/content/questions.json).
 * Fails loudly on broken content instead of skipping entries, so a bad package never replaces
 * working content.
 */
object QuestionContentParser {
    fun parse(json: String): QuestionContent {
        val root = JSONObject(json)
        val categories = root.getJSONArray("categories").objects().map(::parseCategory)
        val questions = root.getJSONArray("questions").objects().map(::parseQuestion)
        validate(categories, questions)
        return QuestionContent(
            contentVersion = root.getInt("contentVersion"),
            categories = categories,
            questions = questions,
        )
    }

    private fun parseCategory(json: JSONObject) =
        ContentCategory(
            id = json.getString("id"),
            name = json.getString("name"),
            order = json.getInt("order"),
        )

    private fun parseQuestion(json: JSONObject): ContentQuestion {
        val answers = json.getJSONArray("answers")
        return ContentQuestion(
            id = json.getString("id"),
            categoryId = json.getString("categoryId"),
            level = json.getInt("level"),
            order = json.getInt("order"),
            title = json.getString("title"),
            answers = (0 until answers.length()).map(answers::getString),
            correctAnswerIndex = json.getInt("correctAnswerIndex"),
            explanation = json.getString("explanation"),
            ageFrom = json.getInt("ageFrom"),
            ageTo = json.getInt("ageTo"),
            difficulty = json.optString("difficulty").takeIf { it.isNotEmpty() }?.let(Difficulty::valueOf),
        )
    }

    private fun validate(
        categories: List<ContentCategory>,
        questions: List<ContentQuestion>,
    ) {
        val categoryIds = categories.map { it.id }.toSet()
        require(categoryIds.size == categories.size) { "Duplicate category ids" }
        val duplicateIds = questions.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicateIds.isEmpty()) { "Duplicate question ids: $duplicateIds" }
        questions.forEach { question ->
            require(question.categoryId in categoryIds) { "Unknown category in ${question.id}" }
            require(question.level >= 1) { "Level must start at 1 in ${question.id}" }
            require(
                question.correctAnswerIndex in question.answers.indices,
            ) { "Invalid answer index in ${question.id}" }
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(this::getJSONObject)
}
