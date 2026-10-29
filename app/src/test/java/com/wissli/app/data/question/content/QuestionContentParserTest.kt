package com.wissli.app.data.question.content

import com.wissli.app.core.model.Difficulty
import com.wissli.app.core.model.QUESTIONS_PER_LEVEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class QuestionContentParserTest {
    @Test
    fun `parses the manifest and each category file`() {
        val content =
            parse(
                "nature" to categoryFile("nature", question("nature-0001", difficulty = "\"HARD\"")),
                "space" to categoryFile("space", question("space-0001")),
            )

        assertEquals(3, content.contentVersion)
        assertEquals(listOf("nature", "space"), content.categories.map { it.id })
        val question = content.questions.first()
        assertEquals("nature-0001", question.id)
        assertEquals("nature", question.categoryId)
        assertEquals(listOf("a", "b", "c", "d"), question.answers)
        assertEquals(Difficulty.HARD, question.difficulty)
        assertEquals("space", content.questions.last().categoryId)
    }

    @Test
    fun `difficulty is optional`() {
        val content =
            parse(
                "nature" to categoryFile("nature", question("nature-0001")),
                "space" to categoryFile("space"),
            )

        assertNull(content.questions.single().difficulty)
    }

    @Test
    fun `rejects duplicate question ids`() {
        assertThrows(IllegalArgumentException::class.java) {
            parse(
                "nature" to categoryFile("nature", question("nature-0001"), question("nature-0001")),
                "space" to categoryFile("space"),
            )
        }
    }

    @Test
    fun `rejects an id of another category`() {
        assertThrows(IllegalArgumentException::class.java) {
            parse("nature" to categoryFile("nature", question("space-0001")), "space" to categoryFile("space"))
        }
    }

    @Test
    fun `rejects a file that belongs to another category`() {
        assertThrows(IllegalArgumentException::class.java) {
            parse("nature" to categoryFile("space"), "space" to categoryFile("space"))
        }
    }

    @Test
    fun `rejects an answer index outside the answers`() {
        assertThrows(IllegalArgumentException::class.java) {
            parse(
                "nature" to categoryFile("nature", question("nature-0001", correctAnswerIndex = 4)),
                "space" to categoryFile("space"),
            )
        }
    }

    // Guards the shipped package: a broken asset would only show up as missing content on a device.
    @Test
    fun `bundled package is valid and fills complete levels`() {
        val content =
            QuestionContentParser.parse(File(BUNDLED_CONTENT, "manifest.json").readText()) { categoryId ->
                File(BUNDLED_CONTENT, "categories/$categoryId.json").readText()
            }

        content.categories.forEach { category ->
            assertTrue("${category.id} has no questions", content.questions.any { it.categoryId == category.id })
        }
        content.questions
            .groupBy { it.categoryId to it.level }
            .forEach { (key, questions) ->
                assertEquals("Level $key", QUESTIONS_PER_LEVEL, questions.size)
                assertEquals(
                    "Order of level $key",
                    (1..QUESTIONS_PER_LEVEL).toList(),
                    questions.map { it.order }.sorted(),
                )
            }
    }

    private fun parse(vararg files: Pair<String, String>): QuestionContent {
        val filesById = files.toMap()
        return QuestionContentParser.parse(MANIFEST) { categoryId -> filesById.getValue(categoryId) }
    }

    private fun categoryFile(
        categoryId: String,
        vararg questions: String,
    ) = """{ "categoryId": "$categoryId", "questions": [${questions.joinToString()}] }"""

    private fun question(
        id: String,
        correctAnswerIndex: Int = 1,
        difficulty: String? = null,
    ) = """
        {
          "id": "$id",
          "level": 1,
          "order": 1,
          "title": "Why?",
          "answers": ["a", "b", "c", "d"],
          "correctAnswerIndex": $correctAnswerIndex,
          "explanation": "Because.",
          "ageFrom": 6,
          "ageTo": 12
          ${difficulty?.let { ", \"difficulty\": $it" }.orEmpty()}
        }
        """

    private companion object {
        // Unit tests run with the module directory as working directory.
        const val BUNDLED_CONTENT = "src/main/assets/content"

        val MANIFEST =
            """
            {
              "contentVersion": 3,
              "categories": [
                { "id": "nature", "name": "Natur", "order": 0 },
                { "id": "space", "name": "Weltall", "order": 1 }
              ]
            }
            """.trimIndent()
    }
}
