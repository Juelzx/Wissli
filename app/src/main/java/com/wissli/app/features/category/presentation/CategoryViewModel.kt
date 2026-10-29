package com.wissli.app.features.category.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.core.model.withProgress
import com.wissli.app.data.progress.ProgressRepository
import com.wissli.app.data.question.QuestionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class CategoryViewModel(
    categoryId: String,
    questionRepository: QuestionRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {
    val state: StateFlow<CategoryState> =
        combine(
            questionRepository.observeCategories(),
            progressRepository.observeProgress(),
        ) { categories, progress ->
            val overview = categories.filter { it.id == categoryId }.withProgress(progress).firstOrNull()
            CategoryState(isLoading = false, overview = overview)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CategoryState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
