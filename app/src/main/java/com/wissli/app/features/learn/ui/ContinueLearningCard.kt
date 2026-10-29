package com.wissli.app.features.learn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.core.model.CategoryOverview
import com.wissli.app.core.model.QUESTIONS_PER_LEVEL
import com.wissli.app.core.model.Rank
import com.wissli.app.ui.components.CardStack
import com.wissli.app.ui.components.RankBadge
import com.wissli.app.ui.components.WissliProgressBar
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.LeafContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.Spacing

/** The one next step on the learning path; shown on the dashboard and on top of the learn tab. */
@Composable
internal fun ContinueLearningCard(
    overview: CategoryOverview,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = overview.progress
    CardStack(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = Spacing.xs,
        ) {
            Column(modifier = Modifier.padding(CardPadding)) {
                ContinueLearningMeta(rank = progress.rank)
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = overview.category.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        stringResource(
                            R.string.learn_level_progress,
                            progress.currentLevel,
                            progress.solvedInCurrentLevel.size,
                            QUESTIONS_PER_LEVEL,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.s))
                WissliProgressBar(progress = progress.solvedInCurrentLevel.size.toFloat() / QUESTIONS_PER_LEVEL)
                Spacer(Modifier.height(Spacing.xs))
                NextRankHint(progress = progress)
                Spacer(Modifier.height(Spacing.m))
                Button(
                    onClick = onContinueClicked,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().heightIn(min = ActionHeight),
                ) {
                    Text(
                        text =
                            if (progress.solvedInCurrentLevel.isEmpty()) {
                                stringResource(R.string.learn_start_level, progress.currentLevel)
                            } else {
                                stringResource(R.string.learn_continue_level, progress.currentLevel)
                            },
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueLearningMeta(rank: Rank) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.learn_continue_label),
            style = MaterialTheme.typography.labelSmall,
            color = OnLeafContainer,
            modifier =
                Modifier
                    .background(LeafContainer, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        )
        Spacer(Modifier.weight(1f))
        RankBadge(rank = rank)
    }
}

private val CardPadding = 18.dp
private val ActionHeight = 56.dp

@ComponentPreviews
@Composable
internal fun ContinueLearningCardPreview() {
    WissliComponentPreview {
        ContinueLearningCard(overview = PreviewOverviews.first(), onContinueClicked = {})
    }
}
