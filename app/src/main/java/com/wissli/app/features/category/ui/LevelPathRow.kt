package com.wissli.app.features.category.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.model.LevelStatus
import com.wissli.app.core.model.QUESTIONS_PER_LEVEL
import com.wissli.app.ui.components.WissliProgressBar
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Spacing

/** One stop on the level path: node plus connector on the left, details on the right. */
@Composable
internal fun LevelPathRow(
    level: Int,
    status: LevelStatus,
    progress: CategoryProgress,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LevelNode(level = level, status = status)
            if (!isLast) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .width(ConnectorWidth)
                            .background(connectorColor(status)),
                )
            }
        }
        LevelDetails(
            level = level,
            status = status,
            progress = progress,
            onClick = onClick,
            modifier = Modifier.weight(1f).padding(bottom = Spacing.s),
        )
    }
}

@Composable
private fun LevelNode(
    level: Int,
    status: LevelStatus,
) {
    val (container, content) =
        when (status) {
            LevelStatus.COMPLETED -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
            LevelStatus.CURRENT -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
            LevelStatus.LOCKED -> MaterialTheme.colorScheme.outlineVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
    Box(
        modifier = Modifier.size(NodeSize).background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (status == LevelStatus.COMPLETED) stringResource(R.string.category_level_done_mark) else "$level",
            style = MaterialTheme.typography.titleMedium,
            color = content,
        )
    }
}

@Composable
private fun LevelDetails(
    level: Int,
    status: LevelStatus,
    progress: CategoryProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    val isCurrent = status == LevelStatus.CURRENT
    Column(
        modifier =
            modifier
                .clip(shape)
                .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, shape)
                .clickable(enabled = status != LevelStatus.LOCKED, role = Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            text = stringResource(R.string.category_level_title, level),
            style = MaterialTheme.typography.titleMedium,
            color =
                if (status == LevelStatus.LOCKED) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
        Text(
            text =
                when (status) {
                    LevelStatus.COMPLETED -> stringResource(R.string.category_level_completed)
                    LevelStatus.CURRENT ->
                        stringResource(
                            R.string.category_level_current,
                            progress.solvedInCurrentLevel.size,
                            QUESTIONS_PER_LEVEL,
                        )
                    LevelStatus.LOCKED -> stringResource(R.string.category_level_locked)
                },
            style = MaterialTheme.typography.labelMedium,
            color =
                if (isCurrent) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        if (isCurrent) {
            Spacer(Modifier.height(Spacing.xxs))
            WissliProgressBar(
                progress = progress.solvedInCurrentLevel.size.toFloat() / QUESTIONS_PER_LEVEL,
                height = CurrentBarHeight,
                trackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            )
        }
    }
}

@Composable
private fun connectorColor(status: LevelStatus): Color =
    if (status ==
        LevelStatus.COMPLETED
    ) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

private val NodeSize = 44.dp
private val ConnectorWidth = 3.dp
private val CurrentBarHeight = 8.dp

@ComponentPreviews
@Composable
internal fun LevelPathRowPreview() {
    val progress = CategoryProgress(categoryId = "nature", completedLevels = 1, solvedInCurrentLevel = setOf("a", "b"))
    WissliComponentPreview {
        Column {
            LevelPathRow(level = 1, status = LevelStatus.COMPLETED, progress = progress, isLast = false, onClick = {})
            LevelPathRow(level = 2, status = LevelStatus.CURRENT, progress = progress, isLast = false, onClick = {})
            LevelPathRow(level = 3, status = LevelStatus.LOCKED, progress = progress, isLast = true, onClick = {})
        }
    }
}
