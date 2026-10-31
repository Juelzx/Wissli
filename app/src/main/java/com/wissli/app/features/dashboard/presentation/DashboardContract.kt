package com.wissli.app.features.dashboard.presentation

import com.wissli.app.core.model.CategoryOverview
import com.wissli.app.core.model.Group
import com.wissli.app.core.model.LearningStats
import com.wissli.app.data.daily.DailyQuestion

data class DashboardState(
    val isLoading: Boolean = true,
    val displayName: String? = null,
    val activeGroup: Group? = null,
    val dailyQuestion: DailyQuestion? = null,
    val continueLearning: CategoryOverview? = null,
    val stats: LearningStats = LearningStats(),
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

    data class DailyAnswerSelected(
        val questionId: String,
        val answerIndex: Int,
    ) : DashboardIntent
}
