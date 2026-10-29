package com.wissli.app.features.level.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.core.model.Rank
import com.wissli.app.features.level.presentation.LevelCompletion
import com.wissli.app.features.level.presentation.LevelIntent
import com.wissli.app.features.level.presentation.LevelState
import com.wissli.app.ui.components.CardStack
import com.wissli.app.ui.components.RankEmblem
import com.wissli.app.ui.components.labelRes
import com.wissli.app.ui.theme.LeafContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.Spacing

@Composable
internal fun LevelCompleteContent(
    state: LevelState,
    completion: LevelCompletion,
    onIntent: (LevelIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Starts collapsed and springs open once, so the result feels earned instead of just appearing.
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val reveal by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    )

    Column(
        modifier = modifier.padding(horizontal = Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
        ) {
            Spacer(Modifier.height(Spacing.l))
            Text(
                text = stringResource(R.string.level_complete_label, state.level),
                style = MaterialTheme.typography.labelSmall,
                color = OnLeafContainer,
                modifier =
                    Modifier
                        .background(LeafContainer, MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            )
            Text(
                text = stringResource(R.string.level_complete_title, state.totalCount),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (state.isReplay) {
                Text(
                    text = stringResource(R.string.level_complete_practice),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                RankResultCard(
                    state = state,
                    completion = completion,
                    reveal = { reveal },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        CompletionActions(state = state, completion = completion, onIntent = onIntent)
    }
}

@Composable
private fun RankResultCard(
    state: LevelState,
    completion: LevelCompletion,
    reveal: () -> Float,
    modifier: Modifier = Modifier,
) {
    val newRank = completion.newRank
    CardStack(modifier = modifier, fanOut = reveal) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = Spacing.xs,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.l).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                if (newRank != null) {
                    Text(
                        text = stringResource(R.string.level_complete_new_rank),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    RankEmblem(
                        rank = newRank,
                        size = EmblemSize,
                        modifier =
                            Modifier.padding(vertical = Spacing.s).graphicsLayer {
                                val scale = MIN_EMBLEM_SCALE + (1f - MIN_EMBLEM_SCALE) * reveal()
                                scaleX = scale
                                scaleY = scale
                            },
                    )
                    Text(
                        text = stringResource(newRank.labelRes()),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.level_complete_rank_topic, state.categoryName),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NextGoal(nextRank = completion.nextRank, levelsToNextRank = completion.levelsToNextRank)
            }
        }
    }
}

@Composable
private fun NextGoal(
    nextRank: Rank?,
    levelsToNextRank: Int,
) {
    Text(
        text =
            if (nextRank != null) {
                pluralStringResource(
                    R.plurals.learn_levels_to_next_rank,
                    levelsToNextRank,
                    levelsToNextRank,
                    stringResource(nextRank.labelRes()),
                )
            } else {
                stringResource(R.string.learn_highest_rank_reached)
            },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CompletionActions(
    state: LevelState,
    completion: LevelCompletion,
    onIntent: (LevelIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (completion.hasNextLevel) {
            Button(
                onClick = { onIntent(LevelIntent.NextLevelClicked) },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.primaryActionHeight),
            ) {
                Text(
                    text = stringResource(R.string.learn_start_level, state.level + 1),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        TextButton(
            onClick = { onIntent(LevelIntent.CloseClicked) },
            modifier = Modifier.fillMaxWidth().heightIn(min = SecondaryActionHeight),
        ) {
            Text(text = stringResource(R.string.level_back_to_overview), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val EmblemSize = 96.dp
private val SecondaryActionHeight = 48.dp
private const val MIN_EMBLEM_SCALE = 0.4f
