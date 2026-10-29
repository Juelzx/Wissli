package com.wissli.app.features.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
    onSignOut: () -> Unit,
    onTabSelected: (MainTab) -> Unit,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        onIntent = viewModel::onIntent,
        onSignOut = onSignOut,
        onTabSelected = onTabSelected,
        onOpenLevel = onOpenLevel,
    )
}

@Composable
private fun DashboardContent(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit,
    onSignOut: () -> Unit,
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
                        onSignOutClicked = onSignOut,
                        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                    )
                }
                learningSection(state = state, onTabSelected = onTabSelected, onOpenLevel = onOpenLevel)
                state.dailyQuestion?.let { question ->
                    item {
                        DailyQuestionCard(
                            question = question,
                            onOpenClicked = { onIntent(DashboardIntent.OpenDailyQuestionClicked) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
                        )
                    }
                }
                membersSection(state = state, onIntent = onIntent)
            }
        }
    }
}

private fun LazyListScope.membersSection(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit,
) {
    item {
        SectionHeader(
            title = stringResource(R.string.dashboard_members_title),
            actionLabel = stringResource(R.string.dashboard_members_manage),
            onActionClicked = { onIntent(DashboardIntent.ManageMembersClicked) },
        )
    }
    item {
        if (state.members.isEmpty()) {
            EmptyHint(text = stringResource(R.string.dashboard_members_empty))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                itemsIndexed(state.members, key = { _, member -> member.userId }) { index, member ->
                    MemberCard(
                        member = member,
                        colorIndex = index,
                        onClick = { onIntent(DashboardIntent.MemberClicked(member.userId)) },
                    )
                }
            }
        }
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
    actionLabel: String,
    onActionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = Spacing.screenHorizontal, end = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onActionClicked) {
            Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
    )
}

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
                members = PreviewMembers,
                continueLearning = PreviewOverviews.first(),
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
            onSignOut = {},
            onTabSelected = {},
            onOpenLevel = { _, _ -> },
        )
    }
}
