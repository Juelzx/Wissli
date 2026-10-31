package com.wissli.app.features.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.core.model.Question
import com.wissli.app.data.daily.DailyQuestion
import com.wissli.app.ui.components.AnswerFeedbackCard
import com.wissli.app.ui.components.AnswerOption
import com.wissli.app.ui.components.CardStack
import com.wissli.app.ui.components.answerLook
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.LeafContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.Spacing

/** Answered right on the dashboard; once answered it shows the result until the next question. */
@Composable
internal fun DailyQuestionCard(
    dailyQuestion: DailyQuestion,
    onAnswerSelected: (answerIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val question = dailyQuestion.question
    CardStack(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = Spacing.xs,
        ) {
            Column(
                modifier = Modifier.padding(CardPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                DailyQuestionMeta(categoryName = dailyQuestion.categoryName)
                Text(
                    text = question.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = Spacing.xxs, bottom = Spacing.xs),
                )
                question.answers.forEachIndexed { index, answer ->
                    AnswerOption(
                        index = index,
                        text = answer,
                        look = answerLook(index, question.correctAnswerIndex, dailyQuestion.answeredIndex),
                        enabled = !dailyQuestion.isAnswered,
                        onClick = { onAnswerSelected(index) },
                    )
                }
                AnimatedVisibility(
                    visible = dailyQuestion.isAnswered,
                    enter = fadeIn() + slideInVertically { it / FEEDBACK_SLIDE_DIVISOR },
                ) {
                    DailyQuestionResult(dailyQuestion = dailyQuestion)
                }
            }
        }
    }
}

@Composable
private fun DailyQuestionMeta(categoryName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.dashboard_daily_question_label),
            style = MaterialTheme.typography.labelSmall,
            color = OnLeafContainer,
            modifier =
                Modifier
                    .background(LeafContainer, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = categoryName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = Spacing.xs),
        )
    }
}

@Composable
private fun DailyQuestionResult(dailyQuestion: DailyQuestion) {
    val isCorrect = dailyQuestion.isAnsweredCorrectly
    Column(
        modifier = Modifier.padding(top = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        AnswerFeedbackCard(
            isCorrect = isCorrect,
            title = stringResource(if (isCorrect) R.string.level_feedback_correct else R.string.level_feedback_wrong),
            explanation = dailyQuestion.question.explanation,
        )
        Text(
            text = stringResource(R.string.dashboard_daily_question_next),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
        )
    }
}

private val CardPadding = 18.dp
private const val FEEDBACK_SLIDE_DIVISOR = 4

@ComponentPreviews
@Composable
internal fun DailyQuestionCardPreview() {
    WissliComponentPreview {
        DailyQuestionCard(dailyQuestion = PreviewDailyQuestion, onAnswerSelected = {})
    }
}

@ComponentPreviews
@Composable
internal fun DailyQuestionCardAnsweredPreview() {
    WissliComponentPreview {
        DailyQuestionCard(dailyQuestion = PreviewDailyQuestion.copy(answeredIndex = 0), onAnswerSelected = {})
    }
}

internal val PreviewDailyQuestion =
    DailyQuestion(
        question =
            Question(
                id = "nature-0001",
                categoryId = "nature",
                level = 1,
                order = 1,
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
        categoryName = "Natur",
        answeredIndex = null,
    )
