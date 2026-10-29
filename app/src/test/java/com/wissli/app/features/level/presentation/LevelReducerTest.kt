package com.wissli.app.features.level.presentation

import com.wissli.app.core.model.Category
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.model.Question
import com.wissli.app.core.model.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelReducerTest {
    @Test
    fun `resumes a level without the questions solved before`() {
        val state = loaded(level = 6, progress = progress(completedLevels = 5, solved = setOf("q1")))

        assertEquals(listOf("q2", "q3"), state.queue.map { it.id })
        assertEquals(1, state.solvedCount)
        assertEquals(3, state.totalCount)
    }

    @Test
    fun `a wrong answer moves the question to the end`() {
        val state =
            loaded(level = 6, progress = progress(completedLevels = 5))
                .reduce(LevelEvent.AnswerSelected(WRONG))
                .reduce(LevelEvent.Continue)

        assertEquals(listOf("q2", "q3", "q1"), state.queue.map { it.id })
        assertEquals(0, state.solvedCount)
        assertNull(state.selectedAnswerIndex)
    }

    @Test
    fun `answers are locked once one was picked`() {
        val state =
            loaded(level = 6, progress = progress(completedLevels = 5))
                .reduce(LevelEvent.AnswerSelected(WRONG))
                .reduce(LevelEvent.AnswerSelected(CORRECT))

        assertEquals(WRONG, state.selectedAnswerIndex)
        assertTrue(state.solvedIds.isEmpty())
    }

    @Test
    fun `solving every question completes the level with a rank-up`() {
        val state = loaded(level = 6, progress = progress(completedLevels = 5)).solveAll()

        assertEquals(
            LevelCompletion(newRank = Rank.EXPERT, nextRank = Rank.MASTER, levelsToNextRank = 4, hasNextLevel = true),
            state.completion,
        )
    }

    @Test
    fun `completing a level without reaching a new rank shows the next goal`() {
        val completion = loaded(level = 2, progress = progress(completedLevels = 1)).solveAll().completion

        assertNull(completion?.newRank)
        assertEquals(Rank.KNOWER, completion?.nextRank)
        assertEquals(1, completion?.levelsToNextRank)
    }

    @Test
    fun `replaying a completed level never changes the rank`() {
        val state = loaded(level = 1, progress = progress(completedLevels = 5, solved = setOf("q1")))

        assertTrue(state.isReplay)
        assertEquals(3, state.queue.size)
        assertNull(state.solveAll().completion?.newRank)
    }

    private fun LevelState.reduce(event: LevelEvent) = LevelReducer.reduce(this, event)

    private fun LevelState.solveAll(): LevelState =
        generateSequence(this) { state ->
            state.reduce(LevelEvent.AnswerSelected(CORRECT)).reduce(LevelEvent.Continue)
        }.first { it.queue.isEmpty() }

    private fun loaded(
        level: Int,
        progress: CategoryProgress,
    ) = LevelState(level = level).reduce(LevelEvent.Loaded(nature, progress, questions(level)))

    private fun progress(
        completedLevels: Int,
        solved: Set<String> = emptySet(),
    ) = CategoryProgress(nature.id, completedLevels = completedLevels, solvedInCurrentLevel = solved)

    private fun questions(level: Int) =
        (3 downTo 1).map { order ->
            Question(
                id = "q$order",
                categoryId = nature.id,
                level = level,
                order = order,
                title = "Question $order",
                answers = listOf("right", "wrong"),
                correctAnswerIndex = CORRECT,
                explanation = "Because.",
            )
        }

    private companion object {
        const val CORRECT = 0
        const val WRONG = 1
        val nature = Category(id = "nature", name = "Nature", order = 0, levelCount = 12)
    }
}
