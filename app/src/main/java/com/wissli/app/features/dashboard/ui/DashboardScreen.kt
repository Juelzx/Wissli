package com.wissli.app.features.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.wissli.app.core.model.Group
import com.wissli.app.core.navigation.MainTab
import com.wissli.app.core.navigation.WissliBottomBar
import com.wissli.app.features.dashboard.presentation.DashboardIntent
import com.wissli.app.features.dashboard.presentation.DashboardState
import com.wissli.app.features.dashboard.presentation.DashboardViewModel
import com.wissli.app.features.learn.ui.ContinueLearningCard
import com.wissli.app.features.learn.ui.PreviewOverviews
import com.wissli.app.ui.theme.Spacing
import com.wissli.app.ui.theme.WissliTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(
    onOpenProfile: () -> Unit,
    onTabSelected: (MainTab) -> Unit,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        onIntent = viewModel::onIntent,
        onOpenProfile = onOpenProfile,
        onTabSelected = onTabSelected,
        onOpenLevel = onOpenLevel,
    )
}

@Composable
private fun DashboardContent(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit,
    onOpenProfile: () -> Unit,
    onTabSelected: (MainTab) -> Unit,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: move the bottom bar to a shared main scaffold once all tabs have screens.
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { WissliBottomBar(selectedTab = MainTab.HOME, onTabSelected = onTabSelected) },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(vertical = Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                item {
                    DashboardHeader(
                        greetingName = state.greetingName,
                        groupName = state.activeGroup?.name,
                        onGroupSwitcherClicked = { onIntent(DashboardIntent.GroupSwitcherClicked) },
                        onProfileClicked = onOpenProfile,
                        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                    )
                }
                learningSection(state = state, onTabSelected = onTabSelected, onOpenLevel = onOpenLevel)
                dailyQuestionSection(state = state, onIntent = onIntent)
                statsSection(state = state)
            }
        }
    }
}

private fun LazyListScope.dailyQuestionSection(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit,
) {
    val daily = state.dailyQuestion ?: return
    item(key = DAILY_QUESTION_KEY) {
        DailyQuestionCard(
            dailyQuestion = daily,
            onAnswerSelected = { index -> onIntent(DashboardIntent.DailyAnswerSelected(daily.question.id, index)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
        )
    }
}

private fun LazyListScope.statsSection(state: DashboardState) {
    item { SectionHeader(title = stringResource(R.string.dashboard_stats_title)) }
    item(key = STATS_KEY) {
        LearningStatsCard(
            stats = state.stats,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
        )
    }
}

// The solo learning path is the main loop of the app, so it leads the dashboard.
private fun LazyListScope.learningSection(
    state: DashboardState,
    onTabSelected: (MainTab) -> Unit,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
) {
    val continueLearning = state.continueLearning ?: return
    item {
        SectionHeader(
            title = stringResource(R.string.dashboard_learning_title),
            actionLabel = stringResource(R.string.dashboard_learning_all),
            onActionClicked = { onTabSelected(MainTab.LEARN) },
        )
    }
    item {
        ContinueLearningCard(
            overview = continueLearning,
            onContinueClicked = {
                onOpenLevel(continueLearning.category.id, continueLearning.progress.currentLevel)
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClicked: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = SectionHeaderMinHeight)
                .padding(start = Spacing.screenHorizontal, end = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null) {
            TextButton(onClick = onActionClicked) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private const val DAILY_QUESTION_KEY = "daily_question"
private const val STATS_KEY = "learning_stats"

// Keeps headers with and without an action button the same height.
private val SectionHeaderMinHeight = 48.dp

@Preview
@Composable
internal fun DashboardPreview() {
    DashboardContentPreview(
        state =
            DashboardState(
                isLoading = false,
                displayName = "Sarah Kaufmann",
                activeGroup =
                    Group(
                        id = "group",
                        name = "Familie Kaufmann",
                        createdBy = "sarah",
                        ownerId = "sarah",
                        createdAt = 0L,
                    ),
                dailyQuestion = PreviewDailyQuestion,
                continueLearning = PreviewOverviews.first(),
                stats = PreviewStats,
            ),
    )
}

@Preview
@Composable
internal fun DashboardEmptyPreview() {
    DashboardContentPreview(state = DashboardState(isLoading = false))
}

@Composable
private fun DashboardContentPreview(state: DashboardState) {
    WissliTheme {
        DashboardContent(
            state = state,
            onIntent = {},
            onOpenProfile = {},
            onTabSelected = {},
            onOpenLevel = { _, _ -> },
        )
    }
}
