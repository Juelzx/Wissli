package com.wissli.app.features.dashboard.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.data.question.DailyQuestion
import com.wissli.app.ui.components.CardStack
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.LavenderContainer
import com.wissli.app.ui.theme.LeafContainer
import com.wissli.app.ui.theme.OnLavenderContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.OnPeachContainer
import com.wissli.app.ui.theme.OnSkyContainer
import com.wissli.app.ui.theme.PeachContainer
import com.wissli.app.ui.theme.SkyContainer
import com.wissli.app.ui.theme.Spacing

@Composable
internal fun DailyQuestionCard(
    question: DailyQuestion,
    onSendAsChallengeClicked: () -> Unit,
    onOpenClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardStack(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = Spacing.xs,
        ) {
            Column(modifier = Modifier.padding(CardPadding)) {
                DailyQuestionMeta(question = question)
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = question.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.m))
                AnswerLetterRow()
                Spacer(Modifier.height(Spacing.m))
                DailyQuestionActions(
                    onSendAsChallengeClicked = onSendAsChallengeClicked,
                    onOpenClicked = onOpenClicked,
                )
            }
        }
    }
}

@Composable
private fun DailyQuestionMeta(question: DailyQuestion) {
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
            text =
                stringResource(
                    R.string.dashboard_daily_question_meta,
                    question.categoryName,
                    question.ageFrom,
                    question.ageTo,
                ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// A teaser of the answer options; the letters alone carry no information for screen readers.
@Composable
private fun AnswerLetterRow() {
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        AnswerLetterColors.forEachIndexed { index, (container, content) ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(AnswerLetterHeight)
                        .background(container, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ('A' + index).toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = content,
                )
            }
        }
    }
}

@Composable
private fun DailyQuestionActions(
    onSendAsChallengeClicked: () -> Unit,
    onOpenClicked: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Button(
            onClick = onSendAsChallengeClicked,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.weight(SEND_BUTTON_WEIGHT).heightIn(min = ActionHeight),
        ) {
            Text(
                text = stringResource(R.string.dashboard_daily_question_send),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onOpenClicked,
            shape = MaterialTheme.shapes.large,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.outlineVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            modifier = Modifier.weight(1f).heightIn(min = ActionHeight),
        ) {
            Text(
                text = stringResource(R.string.dashboard_daily_question_open),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val AnswerLetterColors: List<Pair<Color, Color>> =
    listOf(
        SkyContainer to OnSkyContainer,
        LeafContainer to OnLeafContainer,
        LavenderContainer to OnLavenderContainer,
        PeachContainer to OnPeachContainer,
    )

private val CardPadding = 18.dp
private val AnswerLetterHeight = 40.dp
private val ActionHeight = 56.dp
private const val SEND_BUTTON_WEIGHT = 1.4f

@ComponentPreviews
@Composable
internal fun DailyQuestionCardPreview() {
    WissliComponentPreview {
        DailyQuestionCard(
            question = PreviewDailyQuestion,
            onSendAsChallengeClicked = {},
            onOpenClicked = {},
        )
    }
}

internal val PreviewDailyQuestion =
    DailyQuestion(
        questionId = "sky-blue",
        title = "Warum ist der Himmel blau?",
        categoryName = "Natur",
        ageFrom = 6,
        ageTo = 10,
    )
