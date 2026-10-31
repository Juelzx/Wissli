package com.wissli.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LearningStatsTest {
    private val nature = Category(id = "nature", name = "Nature", order = 0, levelCount = 3)
    private val space = Category(id = "space", name = "Space", order = 1, levelCount = 3)
    private val math = Category(id = "math", name = "Math", order = 2, levelCount = 2)

    @Test
    fun `solved questions come from the progress, including answers before recording started`() {
        val progress =
            mapOf(
                nature.id to CategoryProgress(nature.id, completedLevels = 2, solvedInCurrentLevel = setOf("a", "b")),
                space.id to CategoryProgress(space.id, solvedInCurrentLevel = setOf("c")),
            )

        val stats = learningStats(listOf(nature, space), progress, AnswerHistory(), TODAY)

        assertEquals(2 * QUESTIONS_PER_LEVEL + 3, stats.solvedQuestions)
        assertNull(stats.total.accuracy)
    }

    @Test
    fun `topics are sorted by answers and leave out untouched ones`() {
        val history =
            AnswerHistory(
                byCategory =
                    mapOf(
                        nature.id to AnswerCount(answered = 4, correct = 3),
                        math.id to AnswerCount(answered = 10, correct = 5),
                    ),
            )

        val stats = learningStats(listOf(nature, space, math), emptyMap(), history, TODAY)

        assertEquals(listOf(math, nature), stats.topics.map { it.category })
        assertEquals(AnswerCount(answered = 14, correct = 8), stats.total)
        val natureTopic = stats.topics.last()
        assertEquals(0.75f, natureTopic.count.accuracy)
    }

    @Test
    fun `the last days always cover a full week ending today`() {
        val history = AnswerHistory(answersByDay = mapOf(TODAY to 5, TODAY - 6 to 2, TODAY - 7 to 9))

        val days = learningStats(emptyList(), emptyMap(), history, TODAY).lastDays

        assertEquals(ACTIVITY_DAYS, days.size)
        assertEquals(DayActivity(TODAY - 6, 2), days.first())
        assertEquals(DayActivity(TODAY, 5), days.last())
    }

    @Test
    fun `a streak counts today or, before the first answer of the day, up to yesterday`() {
        assertEquals(3, streakDays(setOf(TODAY, TODAY - 1, TODAY - 2, TODAY - 4), TODAY))
        assertEquals(2, streakDays(setOf(TODAY - 1, TODAY - 2), TODAY))
        assertEquals(0, streakDays(setOf(TODAY - 2), TODAY))
        assertEquals(0, streakDays(emptySet(), TODAY))
    }

    private companion object {
        const val TODAY = 20_757L
    }
}
