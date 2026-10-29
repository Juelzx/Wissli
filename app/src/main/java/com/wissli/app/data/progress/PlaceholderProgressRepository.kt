package com.wissli.app.data.progress

import com.wissli.app.core.model.CategoryProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

// TODO: replace with a Firestore implementation (users/{uid}/progress/{categoryId}).
// Kept in memory, so answers count until the app process ends.
class PlaceholderProgressRepository : ProgressRepository {
    private val progress = MutableStateFlow(emptyMap<String, CategoryProgress>())

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
}
