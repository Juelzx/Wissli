package com.wissli.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Spacing

enum class MainTab(
    @param:StringRes val labelRes: Int,
) {
    HOME(R.string.main_tab_home),
    QUESTIONS(R.string.main_tab_questions),
    CHALLENGES(R.string.main_tab_challenges),
    GROUPS(R.string.main_tab_groups),
}

@Composable
fun WissliBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = Spacing.xs,
    ) {
        Row(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.s, vertical = Spacing.xs)
                    .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            MainTab.entries.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// Dots instead of icons keep the bar calm and avoid pulling in an icon library for four glyphs.
@Composable
private fun BottomBarItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorWidth by animateDpAsState(if (selected) SelectedIndicatorWidth else IndicatorHeight)
    val indicatorColor by animateColorAsState(
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = UNSELECTED_DOT_ALPHA)
        },
    )
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
    )
    val labelColor =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.large)
                .background(background)
                .selectable(selected = selected, onClick = onClick, role = Role.Tab)
                .padding(vertical = Spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            modifier =
                Modifier
                    .width(indicatorWidth)
                    .height(IndicatorHeight)
                    .background(indicatorColor, CircleShape),
        )
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = labelColor,
            maxLines = 1,
        )
    }
}

private val IndicatorHeight = 8.dp
private val SelectedIndicatorWidth = 24.dp
private const val UNSELECTED_DOT_ALPHA = 0.35f

@ComponentPreviews
@Composable
internal fun WissliBottomBarPreview() {
    WissliComponentPreview {
        WissliBottomBar(selectedTab = MainTab.HOME, onTabSelected = {})
    }
}
