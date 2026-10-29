package com.wissli.app.data.question.content

import com.wissli.app.core.model.Difficulty
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses a question package: a manifest (contentVersion + categories) plus one questions file
 * per category, see app/src/main/assets/content/. Fails loudly on broken content instead of
 * skipping entries, so a bad package never replaces working content.
 */
object QuestionContentParser {
    /** [readCategoryFile] returns the questions file of the given category id. */
    fun parse(
        manifestJson: String,
        readCategoryFile: (categoryId: String) -> String,
    ): QuestionContent {
        val manifest = JSONObject(manifestJson)
        val categories = manifest.getJSONArray("categories").objects().map(::parseCategory)
        val questions = categories.flatMap { category -> parseCategoryFile(category.id, readCategoryFile(category.id)) }
        validate(categories, questions)
        return QuestionContent(
            contentVersion = manifest.getInt("contentVersion"),
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

    private fun parseCategoryFile(
        categoryId: String,
        json: String,
    ): List<ContentQuestion> {
        val file = JSONObject(json)
        require(file.getString("categoryId") == categoryId) { "Questions file of $categoryId names another category" }
        return file.getJSONArray("questions").objects().map { parseQuestion(categoryId, it) }
    }

    private fun parseQuestion(
        categoryId: String,
        json: JSONObject,
    ): ContentQuestion {
        val answers = json.getJSONArray("answers")
        return ContentQuestion(
            id = json.getString("id"),
            categoryId = categoryId,
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
        require(categories.map { it.id }.toSet().size == categories.size) { "Duplicate category ids" }
        val duplicateIds = questions.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicateIds.isEmpty()) { "Duplicate question ids: $duplicateIds" }
        questions.forEach { question ->
            // Ids carry their category, so they stay unique even when a question changes level.
            require(
                question.id.startsWith("${question.categoryId}-"),
            ) { "Id ${question.id} does not match its category" }
            require(question.level >= 1) { "Level must start at 1 in ${question.id}" }
            require(
                question.correctAnswerIndex in question.answers.indices,
            ) { "Invalid answer index in ${question.id}" }
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(this::getJSONObject)
}
