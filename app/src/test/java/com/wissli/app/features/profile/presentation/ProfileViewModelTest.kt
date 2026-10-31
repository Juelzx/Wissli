package com.wissli.app.features.profile.presentation

import com.wissli.app.core.model.AppLanguage
import com.wissli.app.core.model.User
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.auth.LastSignedInAccount
import com.wissli.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val settings = FakeSettingsRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the user's name and current settings`() =
        runTest(dispatcher) {
            val viewModel = ProfileViewModel(FakeAuthRepository(), settings)
            backgroundScope.launch { viewModel.state.collect {} }

            advanceUntilIdle()

            assertEquals(ProfileState("Sarah Kaufmann", AppLanguage.GERMAN, true), viewModel.state.value)
        }

    @Test
    fun `changes language and notifications`() =
        runTest(dispatcher) {
            val viewModel = ProfileViewModel(FakeAuthRepository(), settings)
            backgroundScope.launch { viewModel.state.collect {} }

            viewModel.onIntent(ProfileIntent.LanguageSelected(AppLanguage.ENGLISH))
            viewModel.onIntent(ProfileIntent.NotificationsToggled(enabled = false))
            advanceUntilIdle()

            assertEquals(AppLanguage.ENGLISH, viewModel.state.value.language)
            assertFalse(viewModel.state.value.notificationsEnabled)
        }

    private class FakeSettingsRepository : SettingsRepository {
        private val language = MutableStateFlow(AppLanguage.GERMAN)
        private val notifications = MutableStateFlow(true)

        override fun observeLanguage(): StateFlow<AppLanguage> = language.asStateFlow()

        override fun setLanguage(language: AppLanguage) {
            this.language.value = language
        }

        override fun observeNotificationsEnabled(): Flow<Boolean> = notifications

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            notifications.value = enabled
        }
    }

    private class FakeAuthRepository : AuthRepository {
        private val user = flowOf(User(id = "sarah", displayName = "Sarah Kaufmann", createdAt = 0L))

        override fun observeCurrentUser(): Flow<User?> = user

        override fun observeUserId(): Flow<String?> = user.map { it.id }

        override suspend fun getLastSignedInAccount(): LastSignedInAccount? = null

        override suspend fun signInWithGoogle(googleIdToken: String): Result<User> =
            Result.failure(UnsupportedOperationException())

        override suspend fun signOut() = Unit
    }
}
