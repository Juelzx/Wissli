package com.wissli.app.features.level.presentation

import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.model.Question
import com.wissli.app.core.model.Rank

/**
 * [queue] holds the questions still to solve, the first one is on screen. A wrong answer moves the
 * question to the end, so a level only ends once every question has been answered correctly.
 */
data class LevelState(
    val level: Int,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val categoryName: String = "",
    val levelCount: Int = 0,
    val startProgress: CategoryProgress? = null,
    val totalCount: Int = 0,
    val solvedIds: Set<String> = emptySet(),
    val queue: List<Question> = emptyList(),
    val selectedAnswerIndex: Int? = null,
    val completion: LevelCompletion? = null,
) {
    val currentQuestion: Question?
        get() = queue.firstOrNull()

    val isAnswered: Boolean
        get() = selectedAnswerIndex != null

    val isAnswerCorrect: Boolean
        get() = selectedAnswerIndex != null && selectedAnswerIndex == currentQuestion?.correctAnswerIndex

    val solvedCount: Int
        get() = solvedIds.size

    /** Completed levels can be replayed for practice; that never changes progress or rank. */
    val isReplay: Boolean
        get() = startProgress != null && level < startProgress.currentLevel
}

/** [newRank] is only set when finishing this level moved the user up a rank. */
data class LevelCompletion(
    val newRank: Rank?,
    val nextRank: Rank?,
    val levelsToNextRank: Int,
    val hasNextLevel: Boolean,
)

sealed interface LevelIntent {
    data class AnswerSelected(
        val index: Int,
    ) : LevelIntent

    data object ContinueClicked : LevelIntent

    data object RetryClicked : LevelIntent

    data object NextLevelClicked : LevelIntent

    data object CloseClicked : LevelIntent
}

sealed interface LevelEffect {
    data object Close : LevelEffect

    data class OpenLevel(
        val categoryId: String,
        val level: Int,
    ) : LevelEffect

    data object ProgressNotSaved : LevelEffect
}
