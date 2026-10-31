package com.wissli.app.data.daily

import app.cash.turbine.test
import com.wissli.app.core.model.Category
import com.wissli.app.core.model.Question
import com.wissli.app.data.question.QuestionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class LocalDailyQuestionRepositoryTest {
    private val store = FakeStore()

    @Test
    fun `keeps the same question for the whole day`() =
        runTest {
            val morning = repository(at = DAY_START + HOUR).observeDailyQuestion().first()
            val evening = repository(at = DAY_START + 23 * HOUR).observeDailyQuestion().first()

            assertEquals(morning?.question?.id, evening?.question?.id)
            assertEquals("Nature", morning?.categoryName)
        }

    @Test
    fun `stays answered until midnight and only the first answer counts`() =
        runTest {
            val repository = repository(at = DAY_START + HOUR)
            val question = checkNotNull(repository.observeDailyQuestion().first()).question

            repository.answer(question.id, answerIndex = 1)
            repository.answer(question.id, answerIndex = 0)

            val answered = checkNotNull(repository.observeDailyQuestion().first())
            assertTrue(answered.isAnswered)
            assertEquals(1, answered.answeredIndex)
        }

    @Test
    fun `never repeats the previous day's question`() =
        runTest {
            repeat(DAYS_TO_CHECK) { day ->
                val yesterday = store.record.value?.questionId

                val today = repository(at = DAY_START + day * DAY).observeDailyQuestion().first()

                assertNotEquals(yesterday, today?.question?.id)
            }
        }

    @Test
    fun `switches to a new unanswered question at midnight while observed`() =
        runTest {
            val repository = repository(at = DAY_START + 23 * HOUR, followTestTime = true)

            repository.observeDailyQuestion().test {
                val first = checkNotNull(awaitItem())
                repository.answer(first.question.id, answerIndex = 0)
                assertTrue(checkNotNull(awaitItem()).isAnswered)

                advanceTimeBy(HOUR + 1)

                val next = checkNotNull(awaitItem())
                assertNotEquals(first.question.id, next.question.id)
                assertFalse(next.isAnswered)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `without questions there is no daily question`() =
        runTest {
            val repository =
                LocalDailyQuestionRepository(FakeQuestionRepository(ids = emptyList()), store, fixedClock(0))

            assertNull(repository.observeDailyQuestion().first())
        }

    private fun TestScope.repository(
        at: Long,
        followTestTime: Boolean = false,
    ) = LocalDailyQuestionRepository(
        questionRepository = FakeQuestionRepository(ids = listOf("nature-0001", "nature-0002", "nature-0003")),
        store = store,
        clock = if (followTestTime) testClock(at) else fixedClock(at),
        random = Random(SEED),
    )

    private fun fixedClock(millis: Long) = Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC)

    private fun TestScope.testClock(startMillis: Long) =
        object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC

            override fun withZone(zone: ZoneId?): Clock = this

            override fun instant(): Instant = Instant.ofEpochMilli(startMillis + testScheduler.currentTime)
        }

    private class FakeQuestionRepository(
        private val ids: List<String>,
    ) : QuestionRepository {
        override fun observeCategories(): Flow<List<Category>> =
            flowOf(listOf(Category(id = "nature", name = "Nature", order = 0, levelCount = 1)))

        override suspend fun getLevelQuestions(
            categoryId: String,
            level: Int,
        ): List<Question> = emptyList()

        override suspend fun getQuestionIds(): List<String> = ids

        override fun observeQuestion(questionId: String): Flow<Question?> =
            flowOf(
                Question(
                    id = questionId,
                    categoryId = "nature",
                    level = 1,
                    order = 1,
                    title = "Question $questionId",
                    answers = listOf("a", "b"),
                    correctAnswerIndex = 0,
                    explanation = "Because.",
                ).takeIf { questionId in ids },
            )
    }

    private class FakeStore : DailyQuestionStore {
        private val _record = MutableStateFlow<DailyQuestionRecord?>(null)
        override val record: StateFlow<DailyQuestionRecord?> = _record.asStateFlow()

        override suspend fun load(): DailyQuestionRecord? = _record.value

        override suspend fun save(record: DailyQuestionRecord) {
            _record.value = record
        }
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L
        const val DAY = 24 * HOUR

        // 2026-10-31T00:00Z
        const val DAY_START = 20_757 * DAY
        const val DAYS_TO_CHECK = 30
        const val SEED = 42
    }
}
