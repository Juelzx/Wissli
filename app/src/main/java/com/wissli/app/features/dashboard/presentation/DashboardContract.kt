package com.wissli.app.features.dashboard.presentation

import com.wissli.app.core.model.Group
import com.wissli.app.data.challenge.ChallengeSummary
import com.wissli.app.data.group.GroupMemberSummary
import com.wissli.app.data.question.DailyQuestion

data class DashboardState(
    val isLoading: Boolean = true,
    val displayName: String? = null,
    val activeGroup: Group? = null,
    val dailyQuestion: DailyQuestion? = null,
    val members: List<GroupMemberSummary> = emptyList(),
    val openChallenges: List<ChallengeSummary> = emptyList(),
) {
    val greetingName: String?
        get() =
            displayName
                ?.trim()
                ?.substringBefore(' ')
                ?.takeIf { it.isNotBlank() }
}

sealed interface DashboardIntent {
    data object GroupSwitcherClicked : DashboardIntent

    data object SendDailyQuestionClicked : DashboardIntent

    data object OpenDailyQuestionClicked : DashboardIntent

    data object ManageMembersClicked : DashboardIntent

    data class MemberClicked(
        val userId: String,
    ) : DashboardIntent

    data object ShowAllChallengesClicked : DashboardIntent

    data class ChallengeClicked(
        val challengeId: String,
    ) : DashboardIntent
}
