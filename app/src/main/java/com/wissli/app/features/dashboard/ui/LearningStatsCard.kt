package com.wissli.app.features.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.core.model.AnswerCount
import com.wissli.app.core.model.Category
import com.wissli.app.core.model.DayActivity
import com.wissli.app.core.model.LearningStats
import com.wissli.app.core.model.TopicAccuracy
import com.wissli.app.ui.components.WissliProgressBar
import com.wissli.app.ui.components.categoryColors
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.roundToInt

/** Learning overview: totals, the last days and how well each topic goes. */
@Composable
internal fun LearningStatsCard(
    stats: LearningStats,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = Spacing.xs,
    ) {
        Column(modifier = Modifier.padding(Spacing.l)) {
            StatTiles(stats = stats)
            Spacer(Modifier.height(Spacing.l))
            StatsLabel(text = stringResource(R.string.dashboard_stats_last_days))
            ActivityBars(days = stats.lastDays)
            Spacer(Modifier.height(Spacing.l))
            StatsLabel(text = stringResource(R.string.dashboard_stats_topics))
            if (stats.topics.isEmpty()) {
                Text(
                    text = stringResource(R.string.dashboard_stats_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    stats.topics.take(MAX_TOPICS).forEach { topic -> TopicAccuracyRow(topic = topic) }
                }
            }
        }
    }
}

@Composable
private fun StatTiles(stats: LearningStats) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        StatTile(
            value = stats.solvedQuestions.toString(),
            label = stringResource(R.string.dashboard_stats_solved),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = percentText(stats.total),
            label = stringResource(R.string.dashboard_stats_accuracy),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = stats.streakDays.toString(),
            label = pluralStringResource(R.plurals.dashboard_stats_streak, stats.streakDays),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                .padding(Spacing.s),
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatsLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = Spacing.xs),
    )
}

@Composable
private fun ActivityBars(days: List<DayActivity>) {
    val locale = LocalConfiguration.current.locales[0]
    val busiest = days.maxOfOrNull { it.answered }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            // Some locales abbreviate with a trailing dot ("Mo."), which looks noisy under a bar.
            val weekday =
                LocalDate
                    .ofEpochDay(day.epochDay)
                    .dayOfWeek
                    .getDisplayName(TextStyle.SHORT, locale)
                    .removeSuffix(".")
            val description =
                pluralStringResource(R.plurals.dashboard_stats_day_answers, day.answered, weekday, day.answered)
            Column(
                modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ActivityBar(answered = day.answered, busiest = busiest)
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xxs),
                )
            }
        }
    }
}

// Days without answers keep a small stub, so the row still reads as a week.
@Composable
private fun ActivityBar(
    answered: Int,
    busiest: Int,
) {
    val fraction =
        if (answered == 0) EMPTY_BAR_FRACTION else maxOf(answered.toFloat() / busiest, MIN_BAR_FRACTION)
    val color = if (answered == 0) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.fillMaxWidth().height(BarAreaHeight), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .background(color, MaterialTheme.shapes.extraSmall),
        )
    }
}

@Composable
private fun TopicAccuracyRow(topic: TopicAccuracy) {
    val colors = categoryColors(topic.category.order)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = topic.category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text =
                    stringResource(
                        R.string.dashboard_stats_topic_detail,
                        topic.count.correct,
                        topic.count.answered,
                        percentText(topic.count),
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        WissliProgressBar(
            progress = topic.count.accuracy ?: 0f,
            height = TopicBarHeight,
            fillColor = colors.accent,
            trackColor = colors.container,
            modifier = Modifier.padding(top = Spacing.xxs),
        )
    }
}

@Composable
private fun percentText(count: AnswerCount): String =
    count.accuracy?.let { stringResource(R.string.dashboard_stats_percent, (it * PERCENT).roundToInt()) }
        ?: stringResource(R.string.dashboard_stats_no_value)

private const val MAX_TOPICS = 5
private const val PERCENT = 100
private const val EMPTY_BAR_FRACTION = 0.06f
private const val MIN_BAR_FRACTION = 0.15f
private val BarAreaHeight = 56.dp
private val TopicBarHeight = 8.dp

@ComponentPreviews
@Composable
internal fun LearningStatsCardPreview() {
    WissliComponentPreview {
        LearningStatsCard(stats = PreviewStats)
    }
}

@ComponentPreviews
@Composable
internal fun LearningStatsCardEmptyPreview() {
    WissliComponentPreview {
        LearningStatsCard(
            stats = LearningStats(lastDays = (PREVIEW_TODAY - 6..PREVIEW_TODAY).map { DayActivity(it, 0) }),
        )
    }
}

private const val PREVIEW_TODAY = 20_757L

internal val PreviewStats =
    LearningStats(
        solvedQuestions = 128,
        total = AnswerCount(answered = 156, correct = 128),
        streakDays = 4,
        lastDays =
            listOf(
                DayActivity(epochDay = PREVIEW_TODAY - 6, answered = 0),
                DayActivity(epochDay = PREVIEW_TODAY - 5, answered = 12),
                DayActivity(epochDay = PREVIEW_TODAY - 4, answered = 20),
                DayActivity(epochDay = PREVIEW_TODAY - 3, answered = 0),
                DayActivity(epochDay = PREVIEW_TODAY - 2, answered = 8),
                DayActivity(epochDay = PREVIEW_TODAY - 1, answered = 25),
                DayActivity(epochDay = PREVIEW_TODAY, answered = 14),
            ),
        topics =
            listOf(
                TopicAccuracy(
                    category = Category(id = "nature", name = "Natur", order = 0, levelCount = 3),
                    count = AnswerCount(answered = 60, correct = 52),
                ),
                TopicAccuracy(
                    category = Category(id = "space", name = "Weltall", order = 2, levelCount = 3),
                    count = AnswerCount(answered = 41, correct = 30),
                ),
                TopicAccuracy(
                    category = Category(id = "math", name = "Mathe", order = 10, levelCount = 2),
                    count = AnswerCount(answered = 25, correct = 24),
                ),
            ),
    )
