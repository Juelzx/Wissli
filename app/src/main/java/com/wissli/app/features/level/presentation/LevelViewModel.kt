package com.wissli.app.features.level.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.data.progress.ProgressRepository
import com.wissli.app.data.question.QuestionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LevelViewModel(
    private val categoryId: String,
    level: Int,
    private val questionRepository: QuestionRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LevelState(level = level))
    val state: StateFlow<LevelState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<LevelEffect>()
    val effect: SharedFlow<LevelEffect> = _effect

    init {
        load()
    }

    fun onIntent(intent: LevelIntent) {
        when (intent) {
            is LevelIntent.AnswerSelected -> selectAnswer(intent.index)
            LevelIntent.ContinueClicked -> dispatch(LevelEvent.Continue)
            LevelIntent.RetryClicked -> load()
            LevelIntent.NextLevelClicked -> emit(LevelEffect.OpenLevel(categoryId, _state.value.level + 1))
            LevelIntent.CloseClicked -> emit(LevelEffect.Close)
        }
    }

    private fun load() {
        dispatch(LevelEvent.LoadStarted)
        viewModelScope.launch {
            val level = _state.value.level
            val loaded =
                runCatching {
                    val category =
                        questionRepository.observeCategories().first().first { it.id == categoryId }
                    val progress =
                        progressRepository.observeProgress().first()[categoryId] ?: CategoryProgress(categoryId)
                    LevelEvent.Loaded(category, progress, questionRepository.getLevelQuestions(categoryId, level))
                }.getOrNull()
            // Locked levels are not reachable from the UI, so a locked one counts as not loadable.
            dispatch(if (loaded == null || level > loaded.progress.currentLevel) LevelEvent.LoadFailed else loaded)
        }
    }

    private fun selectAnswer(index: Int) {
        val before = _state.value
        dispatch(LevelEvent.AnswerSelected(index))
        val after = _state.value
        val question = before.currentQuestion ?: return
        val newlySolved = question.id in after.solvedIds && question.id !in before.solvedIds
        if (!newlySolved || after.isReplay) return
        viewModelScope.launch {
            progressRepository
                .markSolved(categoryId, after.level, question.id, after.totalCount)
                .onFailure { _effect.emit(LevelEffect.ProgressNotSaved) }
        }
    }

    private fun dispatch(event: LevelEvent) {
        _state.update { LevelReducer.reduce(it, event) }
    }

    private fun emit(effect: LevelEffect) {
        viewModelScope.launch { _effect.emit(effect) }
    }
}
