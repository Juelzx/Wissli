package com.wissli.app.data.progress

import com.wissli.app.core.model.CategoryProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

// TODO: replace with a Firestore implementation (users/{uid}/progress/{categoryId}).
// Kept in memory, so answers count until the app process ends. Nature is seeded two questions
// before a rank-up to make the celebration easy to try.
class PlaceholderProgressRepository : ProgressRepository {
    private val progress =
        MutableStateFlow(
            mapOf(
                "nature" to
                    CategoryProgress(
                        categoryId = "nature",
                        completedLevels = 5,
                        solvedInCurrentLevel = (1..18).map { "nature-6-$it" }.toSet(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                "body" to
                    CategoryProgress(
                        categoryId = "body",
                        completedLevels = 2,
                        solvedInCurrentLevel = (1..7).map { "body-3-$it" }.toSet(),
                        updatedAt = System.currentTimeMillis() - ONE_DAY_MILLIS,
                    ),
            ),
        )

    override fun observeProgress(): Flow<Map<String, CategoryProgress>> = progress.asStateFlow()

    override suspend fun markSolved(
        categoryId: String,
        level: Int,
        questionId: String,
        levelQuestionCount: Int,
    ): Result<CategoryProgress> {
        val updated =
            progress.updateAndGet { all ->
                val current = all[categoryId] ?: CategoryProgress(categoryId)
                all +
                    (
                        categoryId to
                            current.withSolved(
                                level,
                                questionId,
                                levelQuestionCount,
                                System.currentTimeMillis(),
                            )
                    )
            }
        return Result.success(updated.getValue(categoryId))
    }

    private companion object {
        const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
