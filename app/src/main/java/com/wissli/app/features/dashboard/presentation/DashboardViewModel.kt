package com.wissli.app.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.core.model.CategoryOverview
import com.wissli.app.core.model.Group
import com.wissli.app.core.model.LearningStats
import com.wissli.app.core.model.User
import com.wissli.app.core.model.continueCandidate
import com.wissli.app.core.model.withProgress
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.daily.DailyQuestionRepository
import com.wissli.app.data.group.GroupRepository
import com.wissli.app.data.progress.ProgressRepository
import com.wissli.app.data.question.QuestionRepository
import com.wissli.app.data.stats.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    authRepository: AuthRepository,
    groupRepository: GroupRepository,
    questionRepository: QuestionRepository,
    progressRepository: ProgressRepository,
    private val dailyQuestionRepository: DailyQuestionRepository,
    statsRepository: StatsRepository,
) : ViewModel() {
    // Starts with null so a slow or failing user document never blocks the rest of the dashboard.
    private val currentUser: Flow<User?> =
        authRepository
            .observeCurrentUser()
            .onStart { emit(null) }
            .catch { emit(null) }

    // TODO: remember the group the user picked instead of always showing the first one.
    private val activeGroup: Flow<Group?> = groupRepository.observeGroups().map { groups -> groups.firstOrNull() }

    private val continueLearning: Flow<CategoryOverview?> =
        combine(
            questionRepository.observeCategories(),
            progressRepository.observeProgress(),
        ) { categories, progress -> categories.withProgress(progress).continueCandidate() }

    val state: StateFlow<DashboardState> =
        combine(
            currentUser,
            activeGroup,
            dailyQuestionRepository.observeDailyQuestion().catch { emit(null) },
            continueLearning,
            statsRepository.observeStats().catch { emit(LearningStats()) },
        ) { user, group, dailyQuestion, learning, stats ->
            DashboardState(
                isLoading = false,
                displayName = user?.displayName,
                activeGroup = group,
                dailyQuestion = dailyQuestion,
                continueLearning = learning,
                stats = stats,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardState(),
        )

    // TODO: navigate to the group features once they exist.
    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.DailyAnswerSelected ->
                viewModelScope.launch { dailyQuestionRepository.answer(intent.questionId, intent.answerIndex) }

            DashboardIntent.GroupSwitcherClicked -> Unit
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
