package com.wissli.app.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.core.model.CategoryOverview
import com.wissli.app.core.model.Group
import com.wissli.app.core.model.User
import com.wissli.app.core.model.continueCandidate
import com.wissli.app.core.model.withProgress
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.group.GroupMemberSummary
import com.wissli.app.data.group.GroupRepository
import com.wissli.app.data.progress.ProgressRepository
import com.wissli.app.data.question.QuestionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    authRepository: AuthRepository,
    groupRepository: GroupRepository,
    questionRepository: QuestionRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {
    // Starts with null so a slow or failing user document never blocks the rest of the dashboard.
    private val currentUser: Flow<User?> =
        authRepository
            .observeCurrentUser()
            .onStart { emit(null) }
            .catch { emit(null) }

    // TODO: remember the group the user picked instead of always showing the first one.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeGroupContent: Flow<GroupContent> =
        groupRepository
            .observeGroups()
            .map { groups -> groups.firstOrNull() }
            .flatMapLatest { group ->
                if (group == null) {
                    flowOf(GroupContent(group = null, members = emptyList()))
                } else {
                    groupRepository.observeMembers(group.id).map { members -> GroupContent(group, members) }
                }
            }

    private val continueLearning: Flow<CategoryOverview?> =
        combine(
            questionRepository.observeCategories(),
            progressRepository.observeProgress(),
        ) { categories, progress -> categories.withProgress(progress).continueCandidate() }

    val state: StateFlow<DashboardState> =
        combine(
            currentUser,
            activeGroupContent,
            questionRepository.observeDailyQuestion(),
            continueLearning,
        ) { user, groupContent, dailyQuestion, learning ->
            DashboardState(
                isLoading = false,
                displayName = user?.displayName,
                activeGroup = groupContent.group,
                dailyQuestion = dailyQuestion,
                members = groupContent.members,
                continueLearning = learning,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardState(),
        )

    // TODO: navigate to the question and group features once they exist.
    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.GroupSwitcherClicked,
            DashboardIntent.OpenDailyQuestionClicked,
            DashboardIntent.ManageMembersClicked,
            is DashboardIntent.MemberClicked,
            -> Unit
        }
    }

    private data class GroupContent(
        val group: Group?,
        val members: List<GroupMemberSummary>,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
