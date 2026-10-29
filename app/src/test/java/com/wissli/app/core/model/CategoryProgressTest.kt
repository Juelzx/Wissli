package com.wissli.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CategoryProgressTest {
    @Test
    fun `ranks follow completed levels`() {
        assertEquals(Rank.NEWCOMER, Rank.forCompletedLevels(0))
        assertEquals(Rank.EXPLORER, Rank.forCompletedLevels(1))
        assertEquals(Rank.EXPLORER, Rank.forCompletedLevels(2))
        assertEquals(Rank.EXPERT, Rank.forCompletedLevels(6))
        assertEquals(Rank.MASTER, Rank.forCompletedLevels(42))
    }

    @Test
    fun `solving the last open question completes the level`() {
        val progress = CategoryProgress("nature", completedLevels = 5, solvedInCurrentLevel = setOf("a", "b"))

        val afterOne = progress.withSolved(level = 6, questionId = "c", levelQuestionCount = 4, now = 1L)
        val afterAll = afterOne.withSolved(level = 6, questionId = "d", levelQuestionCount = 4, now = 2L)

        assertEquals(setOf("a", "b", "c"), afterOne.solvedInCurrentLevel)
        assertEquals(6, afterAll.completedLevels)
        assertEquals(emptySet<String>(), afterAll.solvedInCurrentLevel)
        assertEquals(Rank.EXPERT, afterAll.rank)
        assertEquals(2L, afterAll.updatedAt)
    }

    @Test
    fun `replayed levels and repeated answers leave progress untouched`() {
        val progress = CategoryProgress("nature", completedLevels = 2, solvedInCurrentLevel = setOf("a"))

        assertSame(progress, progress.withSolved(level = 1, questionId = "x", levelQuestionCount = 20, now = 1L))
        assertSame(progress, progress.withSolved(level = 3, questionId = "a", levelQuestionCount = 20, now = 1L))
    }

    @Test
    fun `shows the way to the next rank`() {
        val explorer = CategoryProgress("nature", completedLevels = 2)

        assertEquals(1, explorer.levelsToNextRank)
        assertEquals(0.5f, explorer.nextRankProgress)
        assertNull(CategoryProgress("nature", completedLevels = 10).levelsToNextRank)
    }

    @Test
    fun `continues with the most recently played open category`() {
        val categories = listOf(category("nature"), category("space"), category("body", levelCount = 2))
        val progress =
            mapOf(
                "nature" to CategoryProgress("nature", solvedInCurrentLevel = setOf("a"), updatedAt = 1L),
                "space" to CategoryProgress("space", completedLevels = 1, updatedAt = 5L),
                "body" to CategoryProgress("body", completedLevels = 2, updatedAt = 9L),
            )

        assertEquals(
            "space",
            categories
                .withProgress(progress)
                .continueCandidate()
                ?.category
                ?.id,
        )
    }

    @Test
    fun `starts with the first category when nothing was played yet`() {
        val categories = listOf(category("nature"), category("space"))

        assertEquals(
            "nature",
            categories
                .withProgress(emptyMap())
                .continueCandidate()
                ?.category
                ?.id,
        )
    }

    private fun category(
        id: String,
        levelCount: Int = 10,
    ) = Category(id = id, name = id, order = 0, levelCount = levelCount)
}
