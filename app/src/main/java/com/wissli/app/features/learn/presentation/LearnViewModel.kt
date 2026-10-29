package com.wissli.app.features.learn.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.core.model.continueCandidate
import com.wissli.app.core.model.withProgress
import com.wissli.app.data.progress.ProgressRepository
import com.wissli.app.data.question.QuestionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LearnViewModel(
    questionRepository: QuestionRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {
    val state: StateFlow<LearnState> =
        combine(
            questionRepository.observeCategories(),
            progressRepository.observeProgress(),
        ) { categories, progress ->
            val overviews = categories.withProgress(progress)
            LearnState(isLoading = false, categories = overviews, continueWith = overviews.continueCandidate())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = LearnState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
