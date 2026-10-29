package com.wissli.app.features.level.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wissli.app.R
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.model.Question
import com.wissli.app.core.model.Rank
import com.wissli.app.features.level.presentation.LevelCompletion
import com.wissli.app.features.level.presentation.LevelEffect
import com.wissli.app.features.level.presentation.LevelIntent
import com.wissli.app.features.level.presentation.LevelState
import com.wissli.app.features.level.presentation.LevelViewModel
import com.wissli.app.ui.theme.Spacing
import com.wissli.app.ui.theme.WissliTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LevelScreen(
    categoryId: String,
    level: Int,
    onClose: () -> Unit,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
    viewModel: LevelViewModel = koinViewModel { parametersOf(categoryId, level) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val notSavedMessage = stringResource(R.string.level_progress_not_saved)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LevelEffect.Close -> onClose()
                is LevelEffect.OpenLevel -> onOpenLevel(effect.categoryId, effect.level)
                LevelEffect.ProgressNotSaved -> snackbarHostState.showSnackbar(notSavedMessage)
            }
        }
    }

    LevelContent(state = state, snackbarHostState = snackbarHostState, onIntent = viewModel::onIntent)
}

@Composable
private fun LevelContent(
    state: LevelState,
    snackbarHostState: SnackbarHostState,
    onIntent: (LevelIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding).fillMaxSize()
        val completion = state.completion
        val question = state.currentQuestion
        when {
            state.isLoading ->
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            state.loadFailed || (question == null && completion == null) ->
                LoadFailed(onIntent = onIntent, modifier = contentModifier)

            completion != null ->
                LevelCompleteContent(
                    state = state,
                    completion = completion,
                    onIntent = onIntent,
                    modifier = contentModifier,
                )

            question != null ->
                QuestionContent(
                    state = state,
                    question = question,
                    onIntent = onIntent,
                    modifier = contentModifier,
                )
        }
    }
}

@Composable
private fun LoadFailed(
    onIntent: (LevelIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.level_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = { onIntent(LevelIntent.RetryClicked) }, shape = MaterialTheme.shapes.large) {
            Text(text = stringResource(R.string.level_retry))
        }
        TextButton(onClick = { onIntent(LevelIntent.CloseClicked) }) {
            Text(text = stringResource(R.string.level_close))
        }
    }
}

@Preview
@Composable
internal fun LevelQuestionPreview() {
    LevelContentPreview(
        state =
            PreviewLevelState.copy(
                selectedAnswerIndex = 0,
            ),
    )
}

@Preview
@Composable
internal fun LevelCompletePreview() {
    LevelContentPreview(
        state =
            PreviewLevelState.copy(
                queue = emptyList(),
                solvedIds = (1..20).map { "q$it" }.toSet(),
                completion =
                    LevelCompletion(
                        newRank = Rank.EXPERT,
                        nextRank = Rank.MASTER,
                        levelsToNextRank = 4,
                        hasNextLevel = true,
                    ),
            ),
    )
}

@Composable
private fun LevelContentPreview(state: LevelState) {
    WissliTheme {
        LevelContent(state = state, snackbarHostState = remember { SnackbarHostState() }, onIntent = {})
    }
}

private val PreviewLevelState =
    LevelState(
        level = 6,
        isLoading = false,
        categoryName = "Natur",
        levelCount = 12,
        startProgress = CategoryProgress(categoryId = "nature", completedLevels = 5),
        totalCount = 20,
        solvedIds = (1..12).map { "q$it" }.toSet(),
        queue =
            listOf(
                Question(
                    id = "q13",
                    categoryId = "nature",
                    level = 6,
                    order = 13,
                    title = "Warum ist der Himmel blau?",
                    answers =
                        listOf(
                            "Er spiegelt das Meer",
                            "Blaues Licht wird stärker gestreut",
                            "Die Sonne ist blau",
                            "Wegen der Wolken",
                        ),
                    correctAnswerIndex = 1,
                    explanation =
                        "Sonnenlicht enthält alle Farben. Die Luft streut kurzwelliges blaues Licht viel stärker " +
                            "als rotes – deshalb kommt es von überall am Himmel zu uns.",
                ),
            ),
    )
