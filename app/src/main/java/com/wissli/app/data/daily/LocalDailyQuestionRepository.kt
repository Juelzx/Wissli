package com.wissli.app.data.daily

import com.wissli.app.core.time.observeEpochDay
import com.wissli.app.data.question.QuestionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import kotlin.random.Random

/** Picks on the device, so the daily question works offline and costs no Firestore reads. */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalDailyQuestionRepository(
    private val questionRepository: QuestionRepository,
    private val store: DailyQuestionStore,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val random: Random = Random.Default,
) : DailyQuestionRepository {
    private val mutex = Mutex()

    override fun observeDailyQuestion(): Flow<DailyQuestion?> =
        clock.observeEpochDay().flatMapLatest { day ->
            flow {
                val record = recordFor(day)
                if (record == null) emit(null) else emitAll(observe(record.questionId, day))
            }
        }

    override suspend fun answer(
        questionId: String,
        answerIndex: Int,
    ) {
        mutex.withLock {
            val current = store.load() ?: return
            val isOpen = current.questionId == questionId && current.answeredIndex == null
            if (isOpen && current.epochDay == today()) store.save(current.copy(answeredIndex = answerIndex))
        }
    }

    private fun observe(
        questionId: String,
        day: Long,
    ): Flow<DailyQuestion?> =
        combine(
            questionRepository.observeQuestion(questionId),
            questionRepository.observeCategories(),
            store.record,
        ) { question, categories, record ->
            question?.let {
                DailyQuestion(
                    question = it,
                    categoryName = categories.firstOrNull { category -> category.id == it.categoryId }?.name.orEmpty(),
                    answeredIndex =
                        record
                            ?.takeIf
                            { stored -> stored.epochDay == day && stored.questionId == questionId }
                            ?.answeredIndex,
                )
            }
        }

    private suspend fun recordFor(day: Long): DailyQuestionRecord? =
        mutex.withLock {
            val current = store.load()
            val questionIds = questionRepository.getQuestionIds()
            // A content update may have removed today's question; then today gets a new one.
            if (current != null && current.epochDay == day && current.questionId in questionIds) {
                current
            } else {
                pickDailyQuestion(questionIds, previousId = current?.questionId, random)
                    ?.let { questionId -> DailyQuestionRecord(epochDay = day, questionId = questionId) }
                    ?.also { store.save(it) }
            }
        }

    private fun today(): Long = LocalDate.now(clock).toEpochDay()
}

/** Never returns [previousId] again unless it is the only question there is. */
internal fun pickDailyQuestion(
    questionIds: List<String>,
    previousId: String?,
    random: Random,
): String? = questionIds.filter { it != previousId }.ifEmpty { questionIds }.randomOrNull(random)
