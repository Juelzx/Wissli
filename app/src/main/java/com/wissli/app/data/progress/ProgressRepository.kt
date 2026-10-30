package com.wissli.app.data.progress

import com.wissli.app.core.model.CategoryProgress
import kotlinx.coroutines.flow.Flow

/**
 * Solo progress of the signed-in user, keyed by category id. Stored on the device and mirrored to
 * one Firestore document per category at users/{uid}/progress/{categoryId}, written only for
 * correct answers.
 */
interface ProgressRepository {
    fun observeProgress(): Flow<Map<String, CategoryProgress>>

    suspend fun markSolved(
        categoryId: String,
        level: Int,
        questionId: String,
        levelQuestionCount: Int,
    ): Result<CategoryProgress>
}
