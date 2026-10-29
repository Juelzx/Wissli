package com.wissli.app.features.level.presentation

import com.wissli.app.core.model.Category
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.model.Question
import com.wissli.app.core.model.Rank

sealed interface LevelEvent {
    data object LoadStarted : LevelEvent

    data object LoadFailed : LevelEvent

    data class Loaded(
        val category: Category,
        val progress: CategoryProgress,
        val questions: List<Question>,
    ) : LevelEvent

    data class AnswerSelected(
        val index: Int,
    ) : LevelEvent

    data object Continue : LevelEvent
}

/** Pure state transitions of a level run; saving progress happens in [LevelViewModel]. */
object LevelReducer {
    fun reduce(
        state: LevelState,
        event: LevelEvent,
    ): LevelState =
        when (event) {
            LevelEvent.LoadStarted -> state.copy(isLoading = true, loadFailed = false)
            LevelEvent.LoadFailed -> state.copy(isLoading = false, loadFailed = true)
            is LevelEvent.Loaded -> loaded(state, event)
            is LevelEvent.AnswerSelected -> answered(state, event.index)
            LevelEvent.Continue -> continued(state)
        }

    private fun loaded(
        state: LevelState,
        event: LevelEvent.Loaded,
    ): LevelState {
        if (event.questions.isEmpty()) return reduce(state, LevelEvent.LoadFailed)
        val isReplay = state.level < event.progress.currentLevel
        val alreadySolved =
            if (isReplay) {
                emptySet()
            } else {
                event.questions
                    .map { it.id }
                    .filter { it in event.progress.solvedInCurrentLevel }
                    .toSet()
            }
        val loadedState =
            state.copy(
                isLoading = false,
                loadFailed = false,
                categoryName = event.category.name,
                levelCount = event.category.levelCount,
                startProgress = event.progress,
                totalCount = event.questions.size,
                solvedIds = alreadySolved,
                queue = event.questions.sortedBy { it.order }.filterNot { it.id in alreadySolved },
                selectedAnswerIndex = null,
                completion = null,
            )
        // Only possible if questions were unpublished mid-level; finishing is better than an empty screen.
        return if (loadedState.queue.isEmpty()) loadedState.copy(completion = completion(loadedState)) else loadedState
    }

    private fun answered(
        state: LevelState,
        index: Int,
    ): LevelState {
        val question = state.currentQuestion ?: return state
        if (state.isAnswered || index !in question.answers.indices) return state
        val solved = if (index == question.correctAnswerIndex) state.solvedIds + question.id else state.solvedIds
        return state.copy(selectedAnswerIndex = index, solvedIds = solved)
    }

    private fun continued(state: LevelState): LevelState {
        val question = state.currentQuestion ?: return state
        if (!state.isAnswered) return state
        val remaining = if (state.isAnswerCorrect) state.queue.drop(1) else state.queue.drop(1) + question
        return state.copy(
            queue = remaining,
            selectedAnswerIndex = null,
            completion = if (remaining.isEmpty()) completion(state) else null,
        )
    }

    private fun completion(state: LevelState): LevelCompletion {
        val completedBefore = state.startProgress?.completedLevels ?: 0
        val completedAfter = if (state.isReplay) completedBefore else completedBefore + 1
        val rankBefore = Rank.forCompletedLevels(completedBefore)
        val rankAfter = Rank.forCompletedLevels(completedAfter)
        val nextRank = rankAfter.next
        return LevelCompletion(
            newRank = rankAfter.takeIf { it != rankBefore },
            nextRank = nextRank,
            levelsToNextRank = nextRank?.let { it.minCompletedLevels - completedAfter } ?: 0,
            hasNextLevel = state.level < state.levelCount,
        )
    }
}
