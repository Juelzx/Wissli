package com.wissli.app.features.dashboard.presentation

import com.wissli.app.core.model.Category
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.model.Group
import com.wissli.app.core.model.GroupRole
import com.wissli.app.core.model.Question
import com.wissli.app.core.model.User
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.auth.LastSignedInAccount
import com.wissli.app.data.group.GroupMemberSummary
import com.wissli.app.data.group.GroupRepository
import com.wissli.app.data.progress.ProgressRepository
import com.wissli.app.data.question.DailyQuestion
import com.wissli.app.data.question.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
import org.junit.Assert.assertTrue
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
    fun `combines user, active group content, daily question and learning progress`() =
        runTest(dispatcher) {
            val viewModel = dashboardViewModel(currentUser = flowOf(sarah))
            backgroundScope.launch { viewModel.state.collect {} }

            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals("Sarah", state.greetingName)
            assertEquals(family, state.activeGroup)
            assertEquals(listOf(mila), state.members)
            assertEquals(skyQuestion, state.dailyQuestion)
            assertEquals(nature, state.continueLearning?.category)
            assertEquals(2, state.continueLearning?.progress?.completedLevels)
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
    fun `without a group there are no members`() =
        runTest(dispatcher) {
            val viewModel = dashboardViewModel(currentUser = flowOf(sarah), groups = emptyList())
            backgroundScope.launch { viewModel.state.collect {} }

            advanceUntilIdle()

            val state = viewModel.state.value
            assertNull(state.activeGroup)
            assertTrue(state.members.isEmpty())
        }
}

private val sarah = User(id = "sarah", displayName = "Sarah Kaufmann", createdAt = 0L)

private val family =
    Group(id = "family", name = "Familie Kaufmann", createdBy = "sarah", ownerId = "sarah", createdAt = 0L)

private val mila =
    GroupMemberSummary(
        userId = "mila",
        displayName = "Mila",
        role = GroupRole.MEMBER,
        streakDays = 4,
        points = 120,
        pointsGoal = 150,
    )

private val nature = Category(id = "nature", name = "Nature", order = 0, levelCount = 10)

private val skyQuestion =
    DailyQuestion(questionId = "sky", title = "Why is the sky blue?", categoryName = "Nature", ageFrom = 6, ageTo = 10)

private fun dashboardViewModel(
    currentUser: Flow<User?>,
    groups: List<Group> = listOf(family),
) = DashboardViewModel(
    authRepository = FakeAuthRepository(currentUser),
    groupRepository =
        object : GroupRepository {
            override fun observeGroups(): Flow<List<Group>> = flowOf(groups)

            override fun observeMembers(groupId: String): Flow<List<GroupMemberSummary>> =
                flowOf(if (groupId == family.id) listOf(mila) else emptyList())
        },
    questionRepository =
        object : QuestionRepository {
            override fun observeDailyQuestion(): Flow<DailyQuestion?> = flowOf(skyQuestion)

            override fun observeCategories(): Flow<List<Category>> = flowOf(listOf(nature))

            override suspend fun getLevelQuestions(
                categoryId: String,
                level: Int,
            ): List<Question> = emptyList()
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
