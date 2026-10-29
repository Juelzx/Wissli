package com.wissli.app.features.learn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wissli.app.R
import com.wissli.app.core.model.Category
import com.wissli.app.core.model.CategoryOverview
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.navigation.MainTab
import com.wissli.app.core.navigation.WissliBottomBar
import com.wissli.app.features.learn.presentation.LearnState
import com.wissli.app.features.learn.presentation.LearnViewModel
import com.wissli.app.ui.theme.Spacing
import com.wissli.app.ui.theme.WissliTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun LearnScreen(
    onTabSelected: (MainTab) -> Unit,
    onOpenCategory: (categoryId: String) -> Unit,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
    viewModel: LearnViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LearnContent(
        state = state,
        onTabSelected = onTabSelected,
        onOpenCategory = onOpenCategory,
        onOpenLevel = onOpenLevel,
    )
}

@Composable
private fun LearnContent(
    state: LearnState,
    onTabSelected: (MainTab) -> Unit,
    onOpenCategory: (categoryId: String) -> Unit,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { WissliBottomBar(selectedTab = MainTab.LEARN, onTabSelected = onTabSelected) },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(vertical = Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                item { LearnHeader(state = state) }
                item { ContinueSection(state = state, onOpenLevel = onOpenLevel) }
                if (state.categories.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.learn_categories_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                        )
                    }
                }
                items(state.categories, key = { it.category.id }) { overview ->
                    CategoryCard(
                        overview = overview,
                        onClick = { onOpenCategory(overview.category.id) },
                        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                    )
                }
            }
        }
    }
}

@Composable
private fun LearnHeader(state: LearnState) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            text = stringResource(R.string.learn_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text =
                stringResource(
                    R.string.learn_stats,
                    pluralStringResource(R.plurals.learn_stats_questions, state.solvedQuestions, state.solvedQuestions),
                    pluralStringResource(R.plurals.learn_stats_levels, state.completedLevels, state.completedLevels),
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContinueSection(
    state: LearnState,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
) {
    val continueWith = state.continueWith
    when {
        continueWith != null ->
            ContinueLearningCard(
                overview = continueWith,
                onContinueClicked = {
                    onOpenLevel(continueWith.category.id, continueWith.progress.currentLevel)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
            )

        state.categories.isEmpty() -> LearnHint(text = stringResource(R.string.learn_empty))

        else -> LearnHint(text = stringResource(R.string.learn_all_finished))
    }
}

@Composable
private fun LearnHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
    )
}

@Preview
@Composable
internal fun LearnPreview() {
    WissliTheme {
        LearnContent(
            state =
                LearnState(
                    isLoading = false,
                    categories = PreviewOverviews,
                    continueWith = PreviewOverviews.first(),
                ),
            onTabSelected = {},
            onOpenCategory = {},
            onOpenLevel = { _, _ -> },
        )
    }
}

internal val PreviewOverviews =
    listOf(
        CategoryOverview(
            category = Category(id = "nature", name = "Natur", order = 0, levelCount = 12),
            progress =
                CategoryProgress(
                    categoryId = "nature",
                    completedLevels = 5,
                    solvedInCurrentLevel = (1..18).map { "q$it" }.toSet(),
                ),
        ),
        CategoryOverview(
            category = Category(id = "space", name = "Weltall", order = 1, levelCount = 10),
            progress = CategoryProgress(categoryId = "space"),
        ),
        CategoryOverview(
            category = Category(id = "body", name = "Körper", order = 2, levelCount = 8),
            progress = CategoryProgress(categoryId = "body", completedLevels = 8),
        ),
    )
