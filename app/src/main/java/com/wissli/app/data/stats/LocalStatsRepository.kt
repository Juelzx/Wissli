package com.wissli.app.data.stats

import android.util.Log
import com.wissli.app.core.model.AnswerCount
import com.wissli.app.core.model.AnswerHistory
import com.wissli.app.core.model.LearningStats
import com.wissli.app.core.model.learningStats
import com.wissli.app.core.time.observeEpochDay
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.local.AnswerDao
import com.wissli.app.data.local.AnswerEntity
import com.wissli.app.data.progress.ProgressRepository
import com.wissli.app.data.question.QuestionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.ZonedDateTime

class LocalStatsRepository(
    private val authRepository: AuthRepository,
    private val answerDao: AnswerDao,
    private val questionRepository: QuestionRepository,
    private val progressRepository: ProgressRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : StatsRepository {
    override fun observeStats(): Flow<LearningStats> =
        combine(
            questionRepository.observeCategories(),
            progressRepository.observeProgress(),
            observeHistory(),
            clock.observeEpochDay(),
        ) { categories, progress, history, today -> learningStats(categories, progress, history, today) }

    // The overview is a nice-to-have, so a failed write is logged instead of interrupting the level.
    override suspend fun recordAnswer(
        categoryId: String,
        questionId: String,
        correct: Boolean,
    ) {
        val userId = authRepository.observeUserId().first() ?: return
        val now = ZonedDateTime.now(clock)
        runCatching {
            answerDao.insert(
                AnswerEntity(
                    userId = userId,
                    categoryId = categoryId,
                    questionId = questionId,
                    correct = correct,
                    answeredAt = now.toInstant().toEpochMilli(),
                    epochDay = now.toLocalDate().toEpochDay(),
                ),
            )
        }.onFailure { Log.w(TAG, "Could not record an answer", it) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeHistory(): Flow<AnswerHistory> =
        authRepository.observeUserId().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(AnswerHistory())
            } else {
                combine(
                    answerDao.observeCategoryCounts(userId),
                    answerDao.observeDayCounts(userId, HISTORY_DAYS),
                ) { categories, days ->
                    AnswerHistory(
                        byCategory = categories.associate { it.categoryId to AnswerCount(it.answered, it.correct) },
                        answersByDay = days.associate { it.epochDay to it.answered },
                    )
                }
            }
        }

    private companion object {
        const val TAG = "LocalStatsRepository"

        // Enough for any realistic streak while keeping the query small.
        const val HISTORY_DAYS = 400
    }
}
