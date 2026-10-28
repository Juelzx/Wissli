package com.wissli.app.features.login.presentation

import android.content.Context
import android.content.ContextWrapper
import app.cash.turbine.test
import com.wissli.app.core.model.User
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.auth.GoogleAccountSelection
import com.wissli.app.data.auth.GoogleAuthDataSource
import com.wissli.app.data.auth.GoogleSignInCancelledException
import com.wissli.app.data.auth.LastSignedInAccount
import com.wissli.app.data.auth.NoGoogleAccountException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    // Never used by the fakes; works on the JVM thanks to unitTests.isReturnDefaultValues.
    private val fakeContext: Context = ContextWrapper(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the last signed-in account on start`() =
        runTest(dispatcher) {
            val viewModel = loginViewModel(authRepository = FakeAuthRepository(lastAccount = sarah))

            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoadingLastAccount)
            assertEquals(sarah, viewModel.state.value.lastAccount)
            assertEquals("Sarah", viewModel.state.value.greetingName)
        }

    @Test
    fun `without a last account the first sign-in variant is shown`() =
        runTest(dispatcher) {
            val viewModel = loginViewModel()

            advanceUntilIdle()

            assertEquals(LoginState(isLoadingLastAccount = false), viewModel.state.value)
        }

    @Test
    fun `greetingName is the first name and null without a name`() {
        assertEquals("Sarah", LoginState(lastAccount = sarah).greetingName)
        assertNull(LoginState(lastAccount = sarah.copy(displayName = "  ")).greetingName)
        assertNull(LoginState().greetingName)
    }

    @Test
    fun `continue prefers authorized accounts and navigates home`() =
        runTest(dispatcher) {
            val googleAuthDataSource = FakeGoogleAuthDataSource()
            val viewModel = loginViewModel(googleAuthDataSource = googleAuthDataSource)

            viewModel.effect.test {
                viewModel.onIntent(LoginIntent.ContinueWithGoogleClicked(fakeContext))
                assertEquals(LoginEffect.NavigateToHome, awaitItem())
            }
            assertEquals(listOf(GoogleAccountSelection.PREFER_AUTHORIZED), googleAuthDataSource.requests)
            assertFalse(viewModel.state.value.isSigningIn)
        }

    @Test
    fun `use another account always opens the full account picker`() =
        runTest(dispatcher) {
            val googleAuthDataSource = FakeGoogleAuthDataSource()
            val viewModel = loginViewModel(googleAuthDataSource = googleAuthDataSource)

            viewModel.onIntent(LoginIntent.UseAnotherAccountClicked(fakeContext))
            advanceUntilIdle()

            assertEquals(listOf(GoogleAccountSelection.ANY_ACCOUNT), googleAuthDataSource.requests)
        }

    @Test
    fun `failed sign-in sets SIGN_IN_FAILED`() =
        runTest(dispatcher) {
            val viewModel = loginViewModel(authRepository = FakeAuthRepository(onSignIn = { failingSignIn() }))

            viewModel.onIntent(LoginIntent.ContinueWithGoogleClicked(fakeContext))
            advanceUntilIdle()

            assertEquals(LoginError.SIGN_IN_FAILED, viewModel.state.value.error)
            assertFalse(viewModel.state.value.isSigningIn)
        }

    @Test
    fun `missing Google account sets NO_GOOGLE_ACCOUNT`() =
        runTest(dispatcher) {
            val viewModel =
                loginViewModel(
                    googleAuthDataSource =
                        FakeGoogleAuthDataSource(
                            onRequest = { throw NoGoogleAccountException(IllegalStateException()) },
                        ),
                )

            viewModel.onIntent(LoginIntent.UseAnotherAccountClicked(fakeContext))
            advanceUntilIdle()

            assertEquals(LoginError.NO_GOOGLE_ACCOUNT, viewModel.state.value.error)
        }

    @Test
    fun `cancelled Google picker sets no error`() =
        runTest(dispatcher) {
            val viewModel =
                loginViewModel(
                    googleAuthDataSource =
                        FakeGoogleAuthDataSource(
                            onRequest = { throw GoogleSignInCancelledException(IllegalStateException()) },
                        ),
                )

            viewModel.onIntent(LoginIntent.ContinueWithGoogleClicked(fakeContext))
            advanceUntilIdle()

            assertNull(viewModel.state.value.error)
            assertFalse(viewModel.state.value.isSigningIn)
        }

    @Test
    fun `ErrorShown clears the error`() =
        runTest(dispatcher) {
            val viewModel = loginViewModel(authRepository = FakeAuthRepository(onSignIn = { failingSignIn() }))
            viewModel.onIntent(LoginIntent.ContinueWithGoogleClicked(fakeContext))
            advanceUntilIdle()

            viewModel.onIntent(LoginIntent.ErrorShown)

            assertNull(viewModel.state.value.error)
        }

    @Test
    fun `second click during a running sign-in is ignored`() =
        runTest(dispatcher) {
            val idTokenGate = CompletableDeferred<String>()
            val authRepository = FakeAuthRepository()
            val viewModel =
                loginViewModel(
                    authRepository = authRepository,
                    googleAuthDataSource = FakeGoogleAuthDataSource(onRequest = { idTokenGate.await() }),
                )

            viewModel.onIntent(LoginIntent.ContinueWithGoogleClicked(fakeContext))
            runCurrent()

            viewModel.onIntent(LoginIntent.UseAnotherAccountClicked(fakeContext))
            runCurrent()

            idTokenGate.complete("fake-id-token")
            advanceUntilIdle()

            assertEquals(1, authRepository.signInCallCount)
        }

    private fun loginViewModel(
        authRepository: AuthRepository = FakeAuthRepository(),
        googleAuthDataSource: GoogleAuthDataSource = FakeGoogleAuthDataSource(),
    ) = LoginViewModel(authRepository = authRepository, googleAuthDataSource = googleAuthDataSource)
}

private val testUser =
    User(
        id = "uid-1",
        displayName = "Test User",
        createdAt = 0L,
    )

private fun failingSignIn(): Result<User> = Result.failure(IllegalStateException("boom"))

private val sarah = LastSignedInAccount(displayName = "Sarah Kaufmann", email = "sarah@example.com")

private class FakeAuthRepository(
    private val lastAccount: LastSignedInAccount? = null,
    private val onSignIn: suspend (String) -> Result<User> = { Result.success(testUser) },
) : AuthRepository {
    var signInCallCount = 0
        private set

    override fun observeCurrentUser(): Flow<User?> = flowOf(null)

    override suspend fun getLastSignedInAccount(): LastSignedInAccount? = lastAccount

    override suspend fun signInWithGoogle(googleIdToken: String): Result<User> {
        signInCallCount++
        return onSignIn(googleIdToken)
    }

    override suspend fun signOut() = Unit
}

private class FakeGoogleAuthDataSource(
    private val onRequest: suspend () -> String = { "fake-id-token" },
) : GoogleAuthDataSource {
    val requests = mutableListOf<GoogleAccountSelection>()

    override suspend fun requestGoogleIdToken(
        activityContext: Context,
        selection: GoogleAccountSelection,
    ): String {
        requests += selection
        return onRequest()
    }
}
