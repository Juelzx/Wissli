package com.wissli.app.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.core.model.Group
import com.wissli.app.core.model.User
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.challenge.ChallengeRepository
import com.wissli.app.data.challenge.ChallengeSummary
import com.wissli.app.data.group.GroupMemberSummary
import com.wissli.app.data.group.GroupRepository
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
    challengeRepository: ChallengeRepository,
    questionRepository: QuestionRepository,
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
                    flowOf(GroupContent(group = null, members = emptyList(), openChallenges = emptyList()))
                } else {
                    combine(
                        groupRepository.observeMembers(group.id),
                        challengeRepository.observeOpenChallenges(group.id),
                    ) { members, openChallenges -> GroupContent(group, members, openChallenges) }
                }
            }

    val state: StateFlow<DashboardState> =
        combine(
            currentUser,
            activeGroupContent,
            questionRepository.observeDailyQuestion(),
        ) { user, groupContent, dailyQuestion ->
            DashboardState(
                isLoading = false,
                displayName = user?.displayName,
                activeGroup = groupContent.group,
                dailyQuestion = dailyQuestion,
                members = groupContent.members,
                openChallenges = groupContent.openChallenges,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardState(),
        )

    // TODO: navigate to the question, challenge and group features once they exist.
    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.GroupSwitcherClicked,
            DashboardIntent.SendDailyQuestionClicked,
            DashboardIntent.OpenDailyQuestionClicked,
            DashboardIntent.ManageMembersClicked,
            is DashboardIntent.MemberClicked,
            DashboardIntent.ShowAllChallengesClicked,
            is DashboardIntent.ChallengeClicked,
            -> Unit
        }
    }

    private data class GroupContent(
        val group: Group?,
        val members: List<GroupMemberSummary>,
        val openChallenges: List<ChallengeSummary>,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
