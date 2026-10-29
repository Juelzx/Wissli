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
    fun `parses categories and questions`() {
        val content = QuestionContentParser.parse(packageJson(question(difficulty = "\"HARD\"")))

        assertEquals(3, content.contentVersion)
        assertEquals(listOf(ContentCategory(id = "nature", name = "Natur", order = 0)), content.categories)
        val question = content.questions.single()
        assertEquals("nature-0001", question.id)
        assertEquals(listOf("a", "b", "c", "d"), question.answers)
        assertEquals(Difficulty.HARD, question.difficulty)
    }

    @Test
    fun `difficulty is optional`() {
        val content = QuestionContentParser.parse(packageJson(question()))

        assertNull(content.questions.single().difficulty)
    }

    @Test
    fun `rejects duplicate question ids`() {
        assertThrows(IllegalArgumentException::class.java) {
            QuestionContentParser.parse(packageJson(question(), question()))
        }
    }

    @Test
    fun `rejects questions of unknown categories`() {
        assertThrows(IllegalArgumentException::class.java) {
            QuestionContentParser.parse(packageJson(question(categoryId = "space")))
        }
    }

    @Test
    fun `rejects an answer index outside the answers`() {
        assertThrows(IllegalArgumentException::class.java) {
            QuestionContentParser.parse(packageJson(question(correctAnswerIndex = 4)))
        }
    }

    // Guards the shipped package: a broken asset would only show up as missing content on a device.
    @Test
    fun `bundled package is valid and fills complete levels`() {
        val content = QuestionContentParser.parse(File(BUNDLED_PACKAGE).readText())

        assertTrue(content.questions.isNotEmpty())
        content.questions
            .groupBy { it.categoryId to it.level }
            .forEach { (key, questions) -> assertEquals("Level $key", QUESTIONS_PER_LEVEL, questions.size) }
    }

    private fun packageJson(vararg questions: String) =
        """
        {
          "contentVersion": 3,
          "categories": [{ "id": "nature", "name": "Natur", "order": 0 }],
          "questions": [${questions.joinToString()}]
        }
        """.trimIndent()

    private fun question(
        categoryId: String = "nature",
        correctAnswerIndex: Int = 1,
        difficulty: String? = null,
    ) = """
        {
          "id": "nature-0001",
          "categoryId": "$categoryId",
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
        const val BUNDLED_PACKAGE = "src/main/assets/content/questions.json"
    }
}
