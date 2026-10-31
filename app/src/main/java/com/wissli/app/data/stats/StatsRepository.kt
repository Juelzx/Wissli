package com.wissli.app.data.stats

import com.wissli.app.core.model.LearningStats
import kotlinx.coroutines.flow.Flow

/** Answer history and learning overview of the signed-in user, kept on the device only. */
interface StatsRepository {
    fun observeStats(): Flow<LearningStats>

    suspend fun recordAnswer(
        categoryId: String,
        questionId: String,
        correct: Boolean,
    )
}
