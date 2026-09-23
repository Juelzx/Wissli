package com.wissli.app.feature.auth.login

import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.exceptions.GetCredentialCancellationException
import app.cash.turbine.test
import com.wissli.app.core.model.User
import com.wissli.app.core.model.UserRole
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.auth.GoogleAuthDataSource
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    // requestGoogleIdToken() verlangt einen echten android.content.Context. In reinen JVM-Unit-Tests
    // (kein Robolectric) würde jeder echte Android-SDK-Aufruf "not mocked" werfen — dank
    // testOptions.unitTests.isReturnDefaultValues in app/build.gradle.kts reicht ein simpler
    // ContextWrapper als Platzhalter, dessen Methoden hier ohnehin nie aufgerufen werden.
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
    fun `erfolgreicher Login emittiert NavigateToHome und beendet das Laden`() =
        runTest(dispatcher) {
            val viewModel =
                LoginViewModel(
                    authRepository = FakeAuthRepository(),
                    googleAuthDataSource = FakeGoogleAuthDataSource(),
                )

            viewModel.effect.test {
                viewModel.onIntent(LoginIntent.SignInWithGoogleClicked(fakeContext))
                assertEquals(LoginEffect.NavigateToHome, awaitItem())
            }
            assertEquals(LoginState(), viewModel.state.value)
        }

    @Test
    fun `fehlgeschlagener Login setzt eine Fehlermeldung`() =
        runTest(dispatcher) {
            val viewModel =
                LoginViewModel(
                    authRepository =
                        FakeAuthRepository(onSignIn = { Result.failure(IllegalStateException("boom")) }),
                    googleAuthDataSource = FakeGoogleAuthDataSource(),
                )

            viewModel.onIntent(LoginIntent.SignInWithGoogleClicked(fakeContext))
            advanceUntilIdle()

            assertEquals(LoginState(isLoading = false, errorMessage = "boom"), viewModel.state.value)
        }

    @Test
    fun `abgebrochener Account-Picker setzt keine Fehlermeldung`() =
        runTest(dispatcher) {
            val viewModel =
                LoginViewModel(
                    authRepository = FakeAuthRepository(),
                    googleAuthDataSource =
                        FakeGoogleAuthDataSource(onRequest = { throw GetCredentialCancellationException() }),
                )

            viewModel.onIntent(LoginIntent.SignInWithGoogleClicked(fakeContext))
            advanceUntilIdle()

            assertEquals(LoginState(isLoading = false, errorMessage = null), viewModel.state.value)
        }

    @Test
    fun `ErrorMessageShown loescht die Fehlermeldung`() =
        runTest(dispatcher) {
            val viewModel =
                LoginViewModel(
                    authRepository =
                        FakeAuthRepository(onSignIn = { Result.failure(IllegalStateException("boom")) }),
                    googleAuthDataSource = FakeGoogleAuthDataSource(),
                )
            viewModel.onIntent(LoginIntent.SignInWithGoogleClicked(fakeContext))
            advanceUntilIdle()

            viewModel.onIntent(LoginIntent.ErrorMessageShown)

            assertNull(viewModel.state.value.errorMessage)
        }

    @Test
    fun `zweiter Klick waehrend eines laufenden Logins wird ignoriert`() =
        runTest(dispatcher) {
            val idTokenGate = CompletableDeferred<String>()
            val authRepository = FakeAuthRepository()
            val viewModel =
                LoginViewModel(
                    authRepository = authRepository,
                    googleAuthDataSource = FakeGoogleAuthDataSource(onRequest = { idTokenGate.await() }),
                )

            viewModel.onIntent(LoginIntent.SignInWithGoogleClicked(fakeContext))
            runCurrent() // erster Login läuft bis zum Suspend-Punkt (idTokenGate.await())

            viewModel.onIntent(LoginIntent.SignInWithGoogleClicked(fakeContext)) // sollte verworfen werden
            runCurrent()

            idTokenGate.complete("fake-id-token")
            advanceUntilIdle()

            assertEquals(1, authRepository.signInCallCount)
        }
}

private val testUser =
    User(
        id = "uid-1",
        displayName = "Test Kind",
        role = UserRole.PARENT,
        familyId = null,
        createdAt = 0L,
    )

private class FakeAuthRepository(
    private val onSignIn: suspend (String) -> Result<User> = { Result.success(testUser) },
) : AuthRepository {
    var signInCallCount = 0
        private set

    override fun observeCurrentUser(): Flow<User?> = flowOf(null)

    override suspend fun signInWithGoogle(googleIdToken: String): Result<User> {
        signInCallCount++
        return onSignIn(googleIdToken)
    }

    override suspend fun signOut() = Unit
}

private class FakeGoogleAuthDataSource(
    private val onRequest: suspend () -> String = { "fake-id-token" },
) : GoogleAuthDataSource {
    override suspend fun requestGoogleIdToken(activityContext: Context): String = onRequest()
}
