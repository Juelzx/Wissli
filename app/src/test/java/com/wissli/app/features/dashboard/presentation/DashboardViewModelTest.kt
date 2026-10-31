package com.wissli.app.features.dashboard.presentation

import com.wissli.app.core.model.Category
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.model.Group
import com.wissli.app.core.model.LearningStats
import com.wissli.app.core.model.Question
import com.wissli.app.core.model.User
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.auth.LastSignedInAccount
import com.wissli.app.data.daily.DailyQuestion
import com.wissli.app.data.daily.DailyQuestionRepository
import com.wissli.app.data.group.GroupMemberSummary
import com.wissli.app.data.group.GroupRepository
import com.wissli.app.data.progress.ProgressRepository
import com.wissli.app.data.question.QuestionRepository
import com.wissli.app.data.stats.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
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
    fun `combines user, active group, daily question, learning progress and stats`() =
        runTest(dispatcher) {
            val viewModel = dashboardViewModel(currentUser = flowOf(sarah))
            backgroundScope.launch { viewModel.state.collect {} }

            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals("Sarah", state.greetingName)
            assertEquals(family, state.activeGroup)
            assertEquals(skyQuestion, state.dailyQuestion)
            assertEquals(nature, state.continueLearning?.category)
            assertEquals(2, state.continueLearning?.progress?.completedLevels)
            assertEquals(sampleStats, state.stats)
        }

    @Test
    fun `shows the dashboard even if the user document never loads`() =
        runTest(dispatcher) {
            val viewModel = dashboardViewModel(currentUser = emptyFlow())
            backgroundScope.launch { viewModel.state.collect {} }

            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoading)
            assertNull(viewModel.state.value.greetingName)
            assertEquals(family, viewModel.state.value.activeGroup)
        }

    @Test
    fun `failing stats never block the dashboard`() =
        runTest(dispatcher) {
            val viewModel =
                dashboardViewModel(
                    currentUser = flowOf(sarah),
                    groups = emptyList(),
                    stats = flow { throw IllegalStateException("broken database") },
                )
            backgroundScope.launch { viewModel.state.collect {} }

            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertNull(state.activeGroup)
            assertEquals(LearningStats(), state.stats)
        }

    @Test
    fun `answering the daily question passes the answer on`() =
        runTest(dispatcher) {
            val daily = FakeDailyQuestionRepository()
            val viewModel = dashboardViewModel(currentUser = flowOf(sarah), dailyQuestionRepository = daily)

            viewModel.onIntent(DashboardIntent.DailyAnswerSelected(questionId = "nature-0001", answerIndex = 1))
            advanceUntilIdle()

            assertEquals(listOf("nature-0001" to 1), daily.answers)
        }
}

private val sarah = User(id = "sarah", displayName = "Sarah Kaufmann", createdAt = 0L)

private val family =
    Group(id = "family", name = "Familie Kaufmann", createdBy = "sarah", ownerId = "sarah", createdAt = 0L)

private val sampleStats = LearningStats(solvedQuestions = 42, streakDays = 3)

private val nature = Category(id = "nature", name = "Nature", order = 0, levelCount = 10)

private val skyQuestion =
    DailyQuestion(
        question =
            Question(
                id = "nature-0001",
                categoryId = "nature",
                level = 1,
                order = 1,
                title = "Why is the sky blue?",
                answers = listOf("Scattering", "Reflection"),
                correctAnswerIndex = 0,
                explanation = "Because.",
            ),
        categoryName = "Nature",
        answeredIndex = null,
    )

private class FakeDailyQuestionRepository : DailyQuestionRepository {
    val answers = mutableListOf<Pair<String, Int>>()

    override fun observeDailyQuestion(): Flow<DailyQuestion?> = flowOf(skyQuestion)

    override suspend fun answer(
        questionId: String,
        answerIndex: Int,
    ) {
        answers += questionId to answerIndex
    }
}

private fun dashboardViewModel(
    currentUser: Flow<User?>,
    groups: List<Group> = listOf(family),
    dailyQuestionRepository: DailyQuestionRepository = FakeDailyQuestionRepository(),
    stats: Flow<LearningStats> = flowOf(sampleStats),
) = DashboardViewModel(
    authRepository = FakeAuthRepository(currentUser),
    groupRepository =
        object : GroupRepository {
            override fun observeGroups(): Flow<List<Group>> = flowOf(groups)

            override fun observeMembers(groupId: String): Flow<List<GroupMemberSummary>> = flowOf(emptyList())
        },
    questionRepository =
        object : QuestionRepository {
            override fun observeCategories(): Flow<List<Category>> = flowOf(listOf(nature))

            override suspend fun getLevelQuestions(
                categoryId: String,
                level: Int,
            ): List<Question> = emptyList()

            override suspend fun getQuestionIds(): List<String> = emptyList()

            override fun observeQuestion(questionId: String): Flow<Question?> = flowOf(null)
        },
    progressRepository =
        object : ProgressRepository {
            override fun observeProgress(): Flow<Map<String, CategoryProgress>> =
                flowOf(mapOf(nature.id to CategoryProgress(nature.id, completedLevels = 2)))

            override suspend fun markSolved(
                categoryId: String,
                level: Int,
                questionId: String,
                levelQuestionCount: Int,
            ): Result<CategoryProgress> = Result.failure(UnsupportedOperationException())
        },
    dailyQuestionRepository = dailyQuestionRepository,
    statsRepository =
        object : StatsRepository {
            override fun observeStats(): Flow<LearningStats> = stats

            override suspend fun recordAnswer(
                categoryId: String,
                questionId: String,
                correct: Boolean,
            ) = Unit
        },
)

private class FakeAuthRepository(
    private val currentUser: Flow<User?>,
) : AuthRepository {
    override fun observeCurrentUser(): Flow<User?> = currentUser

    override fun observeUserId(): Flow<String?> = currentUser.map { it?.id }

    override suspend fun getLastSignedInAccount(): LastSignedInAccount? = null

    override suspend fun signInWithGoogle(googleIdToken: String): Result<User> =
        Result.failure(UnsupportedOperationException())

    override suspend fun signOut() = Unit
}
