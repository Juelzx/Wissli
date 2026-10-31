package com.wissli.app.data.question.content

import com.wissli.app.core.model.Difficulty
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses a question package: a manifest (contentVersion, languages, categories) plus one
 * questions file per category, see app/src/main/assets/content/. Fails loudly on broken content
 * instead of skipping entries, so a bad package never replaces working content.
 */
object QuestionContentParser {
    /** [readCategoryFile] returns the questions file of the given category id. */
    fun parse(
        manifestJson: String,
        readCategoryFile: (categoryId: String) -> String,
    ): QuestionContent {
        val manifest = JSONObject(manifestJson)
        val languages = manifest.getJSONArray("languages").strings()
        require(languages.isNotEmpty()) { "The package names no language" }
        val categories = manifest.getJSONArray("categories").objects().map(::parseCategory)
        val questions = categories.flatMap { category -> parseCategoryFile(category.id, readCategoryFile(category.id)) }
        validate(languages, categories, questions)
        return QuestionContent(
            contentVersion = manifest.getInt("contentVersion"),
            languages = languages,
            categories = categories,
            questions = questions,
        )
    }

    private fun parseCategory(json: JSONObject) =
        ContentCategory(
            id = json.getString("id"),
            names = json.getJSONObject("name").toStringMap(),
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
        val texts = json.getJSONObject("texts")
        return ContentQuestion(
            id = json.getString("id"),
            categoryId = categoryId,
            level = json.getInt("level"),
            order = json.getInt("order"),
            correctAnswerIndex = json.getInt("correctAnswerIndex"),
            ageFrom = json.getInt("ageFrom"),
            ageTo = json.getInt("ageTo"),
            difficulty = json.optString("difficulty").takeIf { it.isNotEmpty() }?.let(Difficulty::valueOf),
            texts = texts.keys().asSequence().associateWith { language -> parseText(texts.getJSONObject(language)) },
        )
    }

    private fun parseText(json: JSONObject) =
        ContentQuestionText(
            title = json.getString("title"),
            answers = json.getJSONArray("answers").strings(),
            explanation = json.getString("explanation"),
        )

    private fun validate(
        languages: List<String>,
        categories: List<ContentCategory>,
        questions: List<ContentQuestion>,
    ) {
        require(categories.map { it.id }.toSet().size == categories.size) { "Duplicate category ids" }
        categories.forEach { category ->
            require(category.names.keys.containsAll(languages)) { "Category ${category.id} misses a language" }
        }
        val duplicateIds = questions.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicateIds.isEmpty()) { "Duplicate question ids: $duplicateIds" }
        questions.forEach { question -> validateQuestion(languages, question) }
    }

    private fun validateQuestion(
        languages: List<String>,
        question: ContentQuestion,
    ) {
        // Ids carry their category, so they stay unique even when a question changes level.
        require(question.id.startsWith("${question.categoryId}-")) { "Id ${question.id} does not match its category" }
        require(question.level >= 1) { "Level must start at 1 in ${question.id}" }
        require(question.texts.keys.containsAll(languages)) { "${question.id} misses a language" }
        val answerCounts =
            question.texts.values
                .map { it.answers.size }
                .toSet()
        // The answer index is shared, so every language must list the same number of answers.
        require(answerCounts.size == 1) { "Answer counts differ between languages in ${question.id}" }
        require(question.correctAnswerIndex in 0 until answerCounts.single()) {
            "Invalid answer index in ${question.id}"
        }
    }

    private fun JSONObject.toStringMap(): Map<String, String> = keys().asSequence().associateWith(::getString)

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(this::getJSONObject)

    private fun JSONArray.strings(): List<String> = (0 until length()).map(this::getString)
}
