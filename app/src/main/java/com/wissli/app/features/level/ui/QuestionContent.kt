package com.wissli.app.features.level.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.core.model.Question
import com.wissli.app.features.level.presentation.LevelIntent
import com.wissli.app.features.level.presentation.LevelState
import com.wissli.app.ui.components.WissliProgressBar
import com.wissli.app.ui.theme.CoralContainer
import com.wissli.app.ui.theme.LavenderContainer
import com.wissli.app.ui.theme.Leaf
import com.wissli.app.ui.theme.LeafContainer
import com.wissli.app.ui.theme.OnCoralContainer
import com.wissli.app.ui.theme.OnLavenderContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.OnPeachContainer
import com.wissli.app.ui.theme.OnSkyContainer
import com.wissli.app.ui.theme.PeachContainer
import com.wissli.app.ui.theme.SkyContainer
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
                    look = answerLook(state = state, question = question, index = index),
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
                FeedbackCard(
                    isCorrect = state.isAnswerCorrect,
                    explanation = question.explanation,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
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

private enum class AnswerLook {
    IDLE,
    CORRECT,
    WRONG,
    DIMMED,
}

// After answering, the right answer is always revealed; the explanation below gives it away anyway.
private fun answerLook(
    state: LevelState,
    question: Question,
    index: Int,
): AnswerLook =
    when {
        !state.isAnswered -> AnswerLook.IDLE
        index == question.correctAnswerIndex -> AnswerLook.CORRECT
        index == state.selectedAnswerIndex -> AnswerLook.WRONG
        else -> AnswerLook.DIMMED
    }

@Composable
private fun AnswerOption(
    index: Int,
    text: String,
    look: AnswerLook,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val (letterContainer, letterContent) = LetterColors[index % LetterColors.size]
    val (container, content, border) =
        when (look) {
            AnswerLook.CORRECT -> Triple(LeafContainer, OnLeafContainer, Leaf)
            AnswerLook.WRONG -> Triple(CoralContainer, OnCoralContainer, OnCoralContainer)
            AnswerLook.IDLE, AnswerLook.DIMMED ->
                Triple(
                    MaterialTheme.colorScheme.surfaceContainerLowest,
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.outlineVariant,
                )
        }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = AnswerMinHeight)
                .alpha(if (look == AnswerLook.DIMMED) DIMMED_ALPHA else 1f),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        border = BorderStroke(if (look == AnswerLook.IDLE) 1.dp else 2.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(LetterSize)
                        .background(letterContainer, MaterialTheme.shapes.small)
                        .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ('A' + index).toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = letterContent,
                )
            }
            Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeedbackCard(
    isCorrect: Boolean,
    explanation: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isCorrect) LeafContainer else CoralContainer,
        contentColor = if (isCorrect) OnLeafContainer else OnCoralContainer,
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text =
                    stringResource(if (isCorrect) R.string.level_feedback_correct else R.string.level_feedback_wrong),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (!isCorrect) {
                Text(
                    text = stringResource(R.string.level_feedback_wrong_hint),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(text = explanation, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private val LetterColors: List<Pair<Color, Color>> =
    listOf(
        SkyContainer to OnSkyContainer,
        LeafContainer to OnLeafContainer,
        LavenderContainer to OnLavenderContainer,
        PeachContainer to OnPeachContainer,
    )

private val AnswerMinHeight = 64.dp
private val LetterSize = 40.dp
private const val DIMMED_ALPHA = 0.5f
private const val FEEDBACK_SLIDE_DIVISOR = 4
