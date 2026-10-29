package com.wissli.app.features.level.presentation

import app.cash.turbine.test
import com.wissli.app.core.model.Category
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.model.Question
import com.wissli.app.data.progress.ProgressRepository
import com.wissli.app.data.question.DailyQuestion
import com.wissli.app.data.question.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LevelViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saves only correct answers of the current level`() =
        runTest(dispatcher) {
            val progressRepository = FakeProgressRepository(CategoryProgress(nature.id, completedLevels = 2))
            val viewModel = LevelViewModel(nature.id, 3, FakeQuestionRepository(), progressRepository)
            advanceUntilIdle()

            viewModel.onIntent(LevelIntent.AnswerSelected(WRONG))
            viewModel.onIntent(LevelIntent.ContinueClicked)
            viewModel.onIntent(LevelIntent.AnswerSelected(CORRECT))
            advanceUntilIdle()

            assertEquals(listOf("q2"), progressRepository.saved)
        }

    @Test
    fun `practice runs of completed levels save nothing`() =
        runTest(dispatcher) {
            val progressRepository = FakeProgressRepository(CategoryProgress(nature.id, completedLevels = 2))
            val viewModel = LevelViewModel(nature.id, 1, FakeQuestionRepository(), progressRepository)
            advanceUntilIdle()

            viewModel.onIntent(LevelIntent.AnswerSelected(CORRECT))
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isReplay)
            assertTrue(progressRepository.saved.isEmpty())
        }

    @Test
    fun `a locked level cannot be played`() =
        runTest(dispatcher) {
            val viewModel =
                LevelViewModel(
                    nature.id,
                    5,
                    FakeQuestionRepository(),
                    FakeProgressRepository(CategoryProgress(nature.id, completedLevels = 2)),
                )
            advanceUntilIdle()

            assertTrue(viewModel.state.value.loadFailed)
            assertTrue(
                viewModel.state.value.queue
                    .isEmpty(),
            )
        }

    @Test
    fun `reports progress that could not be saved`() =
        runTest(dispatcher) {
            val progressRepository =
                FakeProgressRepository(CategoryProgress(nature.id, completedLevels = 2), failSaving = true)
            val viewModel = LevelViewModel(nature.id, 3, FakeQuestionRepository(), progressRepository)
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.onIntent(LevelIntent.AnswerSelected(CORRECT))
                assertEquals(LevelEffect.ProgressNotSaved, awaitItem())
            }
        }

    private class FakeQuestionRepository : QuestionRepository {
        override fun observeDailyQuestion(): Flow<DailyQuestion?> = flowOf(null)

        override fun observeCategories(): Flow<List<Category>> = flowOf(listOf(nature))

        override suspend fun getLevelQuestions(
            categoryId: String,
            level: Int,
        ): List<Question> =
            (1..2).map { order ->
                Question(
                    id = "q$order",
                    categoryId = categoryId,
                    level = level,
                    order = order,
                    title = "Question $order",
                    answers = listOf("right", "wrong"),
                    correctAnswerIndex = CORRECT,
                    explanation = "Because.",
                )
            }
    }

    private class FakeProgressRepository(
        private val progress: CategoryProgress,
        private val failSaving: Boolean = false,
    ) : ProgressRepository {
        val saved = mutableListOf<String>()

        override fun observeProgress(): Flow<Map<String, CategoryProgress>> =
            flowOf(
                mapOf(
                    progress.categoryId to progress,
                ),
            )

        override suspend fun markSolved(
            categoryId: String,
            level: Int,
            questionId: String,
            levelQuestionCount: Int,
        ): Result<CategoryProgress> {
            if (failSaving) return Result.failure(IllegalStateException("offline"))
            saved += questionId
            return Result.success(progress)
        }
    }

    private companion object {
        const val CORRECT = 0
        const val WRONG = 1
        val nature = Category(id = "nature", name = "Nature", order = 0, levelCount = 12)
    }
}
