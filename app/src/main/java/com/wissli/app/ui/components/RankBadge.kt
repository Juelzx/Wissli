package com.wissli.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.core.model.Rank
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Ink
import com.wissli.app.ui.theme.LavenderContainer
import com.wissli.app.ui.theme.LeafContainer
import com.wissli.app.ui.theme.OnLavenderContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.OnPeachContainer
import com.wissli.app.ui.theme.OnSkyContainer
import com.wissli.app.ui.theme.Peach
import com.wissli.app.ui.theme.PeachContainer
import com.wissli.app.ui.theme.SkyContainer
import com.wissli.app.ui.theme.Spacing

@StringRes
fun Rank.labelRes(): Int =
    when (this) {
        Rank.NEWCOMER -> R.string.rank_newcomer
        Rank.EXPLORER -> R.string.rank_explorer
        Rank.KNOWER -> R.string.rank_knower
        Rank.EXPERT -> R.string.rank_expert
        Rank.MASTER -> R.string.rank_master
    }

/** Theme-independent like the answer chips, so a rank keeps its color in light and dark mode. */
data class RankColors(
    val container: Color,
    val content: Color,
)

fun Rank.colors(): RankColors =
    when (this) {
        Rank.NEWCOMER -> RankColors(SkyContainer, OnSkyContainer)
        Rank.EXPLORER -> RankColors(LeafContainer, OnLeafContainer)
        Rank.KNOWER -> RankColors(LavenderContainer, OnLavenderContainer)
        Rank.EXPERT -> RankColors(PeachContainer, OnPeachContainer)
        Rank.MASTER -> RankColors(Ink, Peach)
    }

@Composable
fun RankBadge(
    rank: Rank,
    modifier: Modifier = Modifier,
) {
    val colors = rank.colors()
    Row(
        modifier =
            modifier
                .background(colors.container, CircleShape)
                .padding(horizontal = Spacing.s, vertical = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        RankPips(rank = rank, color = colors.content)
        Text(
            text = stringResource(rank.labelRes()).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.content,
        )
    }
}

// One pip per rank step: a quiet way to show how far up the ladder a rank is.
@Composable
private fun RankPips(
    rank: Rank,
    color: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(PipGap)) {
        repeat(rank.ordinal + 1) {
            Box(modifier = Modifier.size(PipSize).background(color, CircleShape))
        }
    }
}

/** Tilted tile in the style of the logo; [reached] = false renders a faded preview of a future rank. */
@Composable
fun RankEmblem(
    rank: Rank,
    modifier: Modifier = Modifier,
    size: Dp = DefaultEmblemSize,
    reached: Boolean = true,
) {
    val colors = rank.colors()
    val label = stringResource(rank.labelRes())
    val shape = MaterialTheme.shapes.medium
    val container = if (reached) colors.container else MaterialTheme.colorScheme.outlineVariant
    val content = if (reached) colors.content else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier =
            modifier
                .size(size)
                .rotate(EMBLEM_TILT_DEGREES)
                .shadow(elevation = if (reached) Spacing.xs else 0.dp, shape = shape)
                .background(container, shape)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(1).uppercase(),
            style = emblemTextStyle(size),
            color = content,
        )
    }
}

@Composable
private fun emblemTextStyle(size: Dp): TextStyle =
    if (size >= LargeEmblemThreshold) {
        MaterialTheme.typography.displaySmall
    } else {
        MaterialTheme.typography.headlineSmall
    }

private val PipSize = 5.dp
private val PipGap = 2.dp
private val DefaultEmblemSize = 56.dp
private val LargeEmblemThreshold = 72.dp
private const val EMBLEM_TILT_DEGREES = -6f

@ComponentPreviews
@Composable
internal fun RankBadgePreview() {
    WissliComponentPreview {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            RankEmblem(rank = Rank.EXPERT)
            RankEmblem(rank = Rank.MASTER, reached = false)
            RankBadge(rank = Rank.EXPERT)
        }
    }
}
