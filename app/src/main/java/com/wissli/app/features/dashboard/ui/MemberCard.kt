package com.wissli.app.features.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.core.model.GroupRole
import com.wissli.app.data.group.GroupMemberSummary
import com.wissli.app.ui.components.InitialAvatar
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Ink
import com.wissli.app.ui.theme.Mint
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.OnPeachContainer
import com.wissli.app.ui.theme.OnSkyContainer
import com.wissli.app.ui.theme.PaperWhite
import com.wissli.app.ui.theme.Peach
import com.wissli.app.ui.theme.Sky
import com.wissli.app.ui.theme.Spacing

/** Bright, theme-independent cards like the login illustration; [colorIndex] cycles the palette. */
@Composable
internal fun MemberCard(
    member: GroupMemberSummary,
    colorIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MemberCardPalette[colorIndex % MemberCardPalette.size]
    Surface(
        onClick = onClick,
        modifier = modifier.width(MemberCardWidth),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.container,
        contentColor = Ink,
    ) {
        Column(modifier = Modifier.padding(Spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialAvatar(
                    name = member.displayName,
                    size = MemberAvatarSize,
                    containerColor = PaperWhite,
                    contentColor = Ink,
                )
                Spacer(Modifier.weight(1f))
                if (member.streakDays > 0) {
                    StreakBadge(days = member.streakDays)
                }
            }
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = member.roleLabel(),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.s))
            PointsBar(points = member.points, goal = member.pointsGoal, fillColor = colors.accent)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.dashboard_member_points, member.points, member.pointsGoal),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun StreakBadge(days: Int) {
    Text(
        text = pluralStringResource(R.plurals.dashboard_member_streak_days, days, days),
        style = MaterialTheme.typography.labelMedium,
        modifier =
            Modifier
                .background(PaperWhite.copy(alpha = TRANSLUCENT_ALPHA), MaterialTheme.shapes.extraSmall)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
    )
}

@Composable
private fun PointsBar(
    points: Int,
    goal: Int,
    fillColor: Color,
) {
    val progress = if (goal > 0) (points.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(PointsBarHeight)
                .background(PaperWhite.copy(alpha = TRANSLUCENT_ALPHA), CircleShape),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(fillColor, CircleShape),
        )
    }
}

@Composable
private fun GroupMemberSummary.roleLabel(): String =
    when (role) {
        GroupRole.OWNER -> stringResource(R.string.group_role_owner)
        GroupRole.MEMBER -> stringResource(R.string.group_role_member)
    }

private data class MemberCardColors(
    val container: Color,
    val accent: Color,
)

private val MemberCardPalette =
    listOf(
        MemberCardColors(container = Sky, accent = OnSkyContainer),
        MemberCardColors(container = Mint, accent = OnLeafContainer),
        MemberCardColors(container = Peach, accent = OnPeachContainer),
    )

private val MemberCardWidth = 172.dp
private val MemberAvatarSize = 40.dp
private val PointsBarHeight = 10.dp
private const val TRANSLUCENT_ALPHA = 0.6f

@ComponentPreviews
@Composable
internal fun MemberCardPreview() {
    WissliComponentPreview {
        MemberCard(member = PreviewMembers.first(), colorIndex = 0, onClick = {})
    }
}

internal val PreviewMembers =
    listOf(
        GroupMemberSummary(
            userId = "jonas",
            displayName = "Jonas",
            role = GroupRole.OWNER,
            streakDays = 2,
            points = 60,
            pointsGoal = 150,
        ),
        GroupMemberSummary(
            userId = "mila",
            displayName = "Mila",
            role = GroupRole.MEMBER,
            streakDays = 4,
            points = 120,
            pointsGoal = 150,
        ),
        GroupMemberSummary(
            userId = "ben",
            displayName = "Ben",
            role = GroupRole.MEMBER,
            streakDays = 0,
            points = 40,
            pointsGoal = 150,
        ),
    )
