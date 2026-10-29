package com.wissli.app.features.category.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wissli.app.R
import com.wissli.app.core.model.Category
import com.wissli.app.core.model.CategoryOverview
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.model.Rank
import com.wissli.app.features.category.presentation.CategoryState
import com.wissli.app.features.category.presentation.CategoryViewModel
import com.wissli.app.features.learn.ui.NextRankHint
import com.wissli.app.ui.components.RankEmblem
import com.wissli.app.ui.components.WissliProgressBar
import com.wissli.app.ui.components.categoryColors
import com.wissli.app.ui.components.labelRes
import com.wissli.app.ui.theme.Ink
import com.wissli.app.ui.theme.PaperWhite
import com.wissli.app.ui.theme.Spacing
import com.wissli.app.ui.theme.WissliTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CategoryScreen(
    categoryId: String,
    onBack: () -> Unit,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
    viewModel: CategoryViewModel = koinViewModel { parametersOf(categoryId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoryContent(state = state, onBack = onBack, onOpenLevel = onOpenLevel)
}

@Composable
private fun CategoryContent(
    state: CategoryState,
    onBack: () -> Unit,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        val overview = state.overview
        when {
            state.isLoading ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            overview == null ->
                Column(modifier = Modifier.padding(innerPadding).padding(Spacing.screenHorizontal)) {
                    BackButton(onBack = onBack)
                    Text(
                        text = stringResource(R.string.category_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            else ->
                LazyColumn(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.xl),
                ) {
                    item {
                        BackButton(onBack = onBack, modifier = Modifier.padding(horizontal = Spacing.xs))
                    }
                    item {
                        CategoryHero(
                            overview = overview,
                            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                        )
                    }
                    item {
                        Text(
                            text = stringResource(R.string.category_levels_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier =
                                Modifier.padding(
                                    start = Spacing.screenHorizontal,
                                    end = Spacing.screenHorizontal,
                                    top = Spacing.l,
                                    bottom = Spacing.s,
                                ),
                        )
                    }
                    items((1..overview.category.levelCount).toList(), key = { it }) { level ->
                        LevelPathRow(
                            level = level,
                            status = overview.levelStatus(level),
                            progress = overview.progress,
                            isLast = level == overview.category.levelCount,
                            onClick = { onOpenLevel(overview.category.id, level) },
                            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                        )
                    }
                }
        }
    }
}

@Composable
private fun BackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onBack, modifier = modifier) {
        Text(text = stringResource(R.string.category_back), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CategoryHero(
    overview: CategoryOverview,
    modifier: Modifier = Modifier,
) {
    val colors = categoryColors(overview.category.order)
    val progress = overview.progress
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.container,
        contentColor = Ink,
    ) {
        Column(modifier = Modifier.padding(Spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.category_rank_label),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = stringResource(progress.rank.labelRes()),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(text = overview.category.name, style = MaterialTheme.typography.titleMedium)
                }
                RankEmblem(rank = progress.rank, size = HeroEmblemSize)
            }
            Spacer(Modifier.height(Spacing.m))
            WissliProgressBar(
                progress = progress.nextRankProgress,
                fillColor = colors.accent,
                trackColor = PaperWhite.copy(alpha = TRACK_ALPHA),
            )
            Spacer(Modifier.height(Spacing.xs))
            NextRankHint(progress = progress, color = Ink)
            Spacer(Modifier.height(Spacing.m))
            RankLadder(current = progress.rank)
        }
    }
}

// Every rank is visible from the start, so the goal ("Experte") is concrete instead of abstract.
@Composable
private fun RankLadder(current: Rank) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Rank.entries.forEach { rank ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RankEmblem(rank = rank, size = LadderEmblemSize, reached = rank <= current)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(rank.labelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }
    }
}

private val HeroEmblemSize = 80.dp
private val LadderEmblemSize = 36.dp
private const val TRACK_ALPHA = 0.6f

@Preview
@Composable
internal fun CategoryPreview() {
    WissliTheme {
        CategoryContent(
            state =
                CategoryState(
                    isLoading = false,
                    overview =
                        CategoryOverview(
                            category = Category(id = "nature", name = "Natur", order = 0, levelCount = 12),
                            progress =
                                CategoryProgress(
                                    categoryId = "nature",
                                    completedLevels = 5,
                                    solvedInCurrentLevel = (1..18).map { "q$it" }.toSet(),
                                ),
                        ),
                ),
            onBack = {},
            onOpenLevel = { _, _ -> },
        )
    }
}
