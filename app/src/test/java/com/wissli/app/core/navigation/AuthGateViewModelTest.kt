package com.wissli.app.core.navigation

import com.wissli.app.core.model.User
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.auth.LastSignedInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthGateViewModelTest {
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
    fun `only the first sign-in state decides the start destination`() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository()
            val viewModel = AuthGateViewModel(authRepository)
            // advanceUntilIdle returns while only backgroundScope work is queued, so the collector would never run.
            backgroundScope.launch { viewModel.state.collect {} }
            runCurrent()
            assertEquals(AuthGateState.LoggedOut, viewModel.state.value)

            authRepository.currentUser.value = testUser
            runCurrent()

            assertEquals(AuthGateState.LoggedOut, viewModel.state.value)
        }

    @Test
    fun `signOut signs out at the repository`() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository()
            val viewModel = AuthGateViewModel(authRepository)

            viewModel.signOut()
            advanceUntilIdle()

            assertTrue(authRepository.signedOut)
        }
}

private val testUser = User(id = "uid-1", displayName = "Test User", createdAt = 0L)

private class FakeAuthRepository : AuthRepository {
    val currentUser = MutableStateFlow<User?>(null)
    var signedOut = false
        private set

    override fun observeCurrentUser(): Flow<User?> = currentUser

    override suspend fun getLastSignedInAccount(): LastSignedInAccount? = null

    override suspend fun signInWithGoogle(googleIdToken: String): Result<User> = Result.success(testUser)

    override suspend fun signOut() {
        signedOut = true
    }
}
