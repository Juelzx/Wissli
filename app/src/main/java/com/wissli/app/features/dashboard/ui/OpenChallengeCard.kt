package com.wissli.app.features.dashboard.ui

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.core.model.ChallengeStatus
import com.wissli.app.data.challenge.ChallengeSummary
import com.wissli.app.ui.components.InitialAvatar
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.CoralContainer
import com.wissli.app.ui.theme.LeafContainer
import com.wissli.app.ui.theme.OnCoralContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.OnSkyContainer
import com.wissli.app.ui.theme.SkyContainer
import com.wissli.app.ui.theme.Spacing
import java.util.concurrent.TimeUnit

@Composable
internal fun OpenChallengeCard(
    challenge: ChallengeSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            InitialAvatar(
                name = challenge.createdByName,
                containerColor = LeafContainer,
                contentColor = OnLeafContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.questionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        stringResource(
                            R.string.dashboard_challenge_from,
                            challenge.createdByName,
                            relativeTime(challenge.createdAt),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusBadge(status = challenge.status)
        }
    }
}

// DateUtils is localized by the platform, so no plural resources are needed for "2 hours ago".
@Composable
private fun relativeTime(timestampMillis: Long): String =
    remember(timestampMillis) {
        DateUtils
            .getRelativeTimeSpanString(timestampMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            .toString()
    }

@Composable
private fun StatusBadge(status: ChallengeStatus) {
    val (labelRes, container, content) =
        when (status) {
            ChallengeStatus.PENDING -> Triple(R.string.challenge_status_pending, CoralContainer, OnCoralContainer)
            ChallengeStatus.IN_PROGRESS -> Triple(R.string.challenge_status_in_progress, SkyContainer, OnSkyContainer)
            ChallengeStatus.COMPLETED -> Triple(R.string.challenge_status_completed, LeafContainer, OnLeafContainer)
        }
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier =
            Modifier
                .background(container, MaterialTheme.shapes.extraSmall)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
    )
}

@ComponentPreviews
@Composable
internal fun OpenChallengeCardPreview() {
    WissliComponentPreview {
        OpenChallengeCard(challenge = previewChallenges().first(), onClick = {})
    }
}

internal fun previewChallenges(): List<ChallengeSummary> {
    val now = System.currentTimeMillis()
    return listOf(
        ChallengeSummary(
            id = "zebra-stripes",
            questionTitle = "Warum haben Zebras Streifen?",
            createdByName = "Mila",
            createdAt = now - TimeUnit.HOURS.toMillis(2),
            status = ChallengeStatus.PENDING,
        ),
        ChallengeSummary(
            id = "moon-shape",
            questionTitle = "Warum ändert der Mond seine Form?",
            createdByName = "Ben",
            createdAt = now - TimeUnit.DAYS.toMillis(1),
            status = ChallengeStatus.IN_PROGRESS,
        ),
    )
}
