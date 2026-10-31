package com.wissli.app.features.level.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wissli.app.R
import com.wissli.app.core.model.Question
import com.wissli.app.features.level.presentation.LevelIntent
import com.wissli.app.features.level.presentation.LevelState
import com.wissli.app.ui.components.AnswerFeedbackCard
import com.wissli.app.ui.components.AnswerOption
import com.wissli.app.ui.components.WissliProgressBar
import com.wissli.app.ui.components.answerLook
import com.wissli.app.ui.theme.LavenderContainer
import com.wissli.app.ui.theme.OnLavenderContainer
import com.wissli.app.ui.theme.Spacing

@Composable
internal fun QuestionContent(
    state: LevelState,
    question: Question,
    onIntent: (LevelIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(question.id) { scrollState.scrollTo(0) }
    Column(modifier = modifier) {
        LevelTopBar(state = state, onCloseClicked = { onIntent(LevelIntent.CloseClicked) })
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            LevelLabel(state = state)
            Text(
                text = question.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xs))
            question.answers.forEachIndexed { index, answer ->
                AnswerOption(
                    index = index,
                    text = answer,
                    look = answerLook(index, question.correctAnswerIndex, state.selectedAnswerIndex),
                    enabled = !state.isAnswered,
                    onClick = { onIntent(LevelIntent.AnswerSelected(index)) },
                )
            }
            AnimatedVisibility(
                visible = state.isAnswered,
                enter = fadeIn() + slideInVertically { it / FEEDBACK_SLIDE_DIVISOR },
                // No exit animation: it would briefly show the next question's explanation.
                exit = ExitTransition.None,
            ) {
                LevelFeedback(state = state, question = question)
            }
        }
        if (state.isAnswered) {
            Button(
                onClick = { onIntent(LevelIntent.ContinueClicked) },
                shape = MaterialTheme.shapes.large,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.m)
                        .heightIn(min = Spacing.primaryActionHeight),
            ) {
                Text(text = stringResource(R.string.level_continue), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun LevelFeedback(
    state: LevelState,
    question: Question,
) {
    val isCorrect = state.isAnswerCorrect
    AnswerFeedbackCard(
        isCorrect = isCorrect,
        title = stringResource(if (isCorrect) R.string.level_feedback_correct else R.string.level_feedback_wrong),
        explanation = question.explanation,
        hint = stringResource(R.string.level_feedback_wrong_hint).takeUnless { isCorrect },
        modifier = Modifier.padding(top = Spacing.xs),
    )
}

@Composable
private fun LevelTopBar(
    state: LevelState,
    onCloseClicked: () -> Unit,
) {
    val closeLabel = stringResource(R.string.level_close)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.xs, end = Spacing.screenHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        IconButton(onClick = onCloseClicked, modifier = Modifier.semantics { contentDescription = closeLabel }) {
            Text(
                text = stringResource(R.string.level_close_mark),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        WissliProgressBar(
            progress = if (state.totalCount > 0) state.solvedCount.toFloat() / state.totalCount else 0f,
            modifier = Modifier.weight(1f),
            fillColor = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = stringResource(R.string.level_counter, state.solvedCount, state.totalCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LevelLabel(state: LevelState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(R.string.level_header, state.categoryName.uppercase(), state.level),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (state.isReplay) {
            Text(
                text = stringResource(R.string.level_practice),
                style = MaterialTheme.typography.labelSmall,
                color = OnLavenderContainer,
                modifier =
                    Modifier
                        .background(LavenderContainer, MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            )
        }
    }
}

private const val FEEDBACK_SLIDE_DIVISOR = 4
