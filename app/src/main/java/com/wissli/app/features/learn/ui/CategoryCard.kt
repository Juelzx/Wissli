package com.wissli.app.features.learn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wissli.app.R
import com.wissli.app.core.model.CategoryOverview
import com.wissli.app.ui.components.RankBadge
import com.wissli.app.ui.components.WissliProgressBar
import com.wissli.app.ui.components.categoryColors
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Ink
import com.wissli.app.ui.theme.PaperWhite
import com.wissli.app.ui.theme.Spacing

/** Bright, theme-independent like the member cards; the bar shows the whole category, not one level. */
@Composable
internal fun CategoryCard(
    overview: CategoryOverview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = categoryColors(overview.category.order)
    val progress = overview.progress
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.container,
        contentColor = Ink,
    ) {
        Column(modifier = Modifier.padding(Spacing.m)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = overview.category.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text =
                            if (overview.isFinished) {
                                stringResource(R.string.learn_category_finished)
                            } else {
                                stringResource(
                                    R.string.learn_category_levels,
                                    progress.currentLevel,
                                    overview.category.levelCount,
                                )
                            },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                RankBadge(rank = progress.rank)
            }
            Spacer(Modifier.height(Spacing.s))
            WissliProgressBar(
                progress =
                    if (overview.category.levelCount > 0) {
                        progress.completedLevels.toFloat() / overview.category.levelCount
                    } else {
                        0f
                    },
                fillColor = colors.accent,
                trackColor = PaperWhite.copy(alpha = TRACK_ALPHA),
            )
        }
    }
}

private const val TRACK_ALPHA = 0.6f

@ComponentPreviews
@Composable
internal fun CategoryCardPreview() {
    WissliComponentPreview {
        CategoryCard(overview = PreviewOverviews.first(), onClick = {})
    }
}
