package com.wissli.app.features.learn.presentation

import com.wissli.app.core.model.CategoryOverview
import com.wissli.app.core.model.QUESTIONS_PER_LEVEL

data class LearnState(
    val isLoading: Boolean = true,
    val categories: List<CategoryOverview> = emptyList(),
    val continueWith: CategoryOverview? = null,
) {
    val completedLevels: Int
        get() = categories.sumOf { it.progress.completedLevels }

    val solvedQuestions: Int
        get() =
            categories.sumOf {
                it.progress.completedLevels * QUESTIONS_PER_LEVEL + it.progress.solvedInCurrentLevel.size
            }
}
