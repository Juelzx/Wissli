package com.wissli.app.features.learn.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wissli.app.R
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.ui.components.labelRes

/** "2 more levels to Expert": the next goal is always in sight. */
@Composable
internal fun NextRankHint(
    progress: CategoryProgress,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val nextRank = progress.rank.next
    val levelsToNext = progress.levelsToNextRank
    Text(
        text =
            if (nextRank != null && levelsToNext != null) {
                pluralStringResource(
                    R.plurals.learn_levels_to_next_rank,
                    levelsToNext,
                    levelsToNext,
                    stringResource(nextRank.labelRes()),
                )
            } else {
                stringResource(R.string.learn_highest_rank_reached)
            },
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier,
    )
}
