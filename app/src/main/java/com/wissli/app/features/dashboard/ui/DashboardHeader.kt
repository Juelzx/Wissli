package com.wissli.app.features.dashboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.ui.components.InitialAvatar
import com.wissli.app.ui.components.WissliLogoTile
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Leaf
import com.wissli.app.ui.theme.Spacing

@Composable
internal fun DashboardHeader(
    greetingName: String?,
    groupName: String?,
    onGroupSwitcherClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        WissliLogoTile()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            GroupSwitcher(groupName = groupName, onClick = onGroupSwitcherClicked)
            Text(
                text =
                    if (greetingName != null) {
                        stringResource(R.string.dashboard_greeting_named, greetingName)
                    } else {
                        stringResource(R.string.dashboard_greeting)
                    },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AccountMenu(name = greetingName.orEmpty(), onSignOutClicked = onSignOutClicked)
    }
}

@Composable
private fun GroupSwitcher(
    groupName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Box(modifier = Modifier.size(GroupDotSize).background(Leaf, MaterialTheme.shapes.extraSmall))
            Text(
                text = groupName ?: stringResource(R.string.dashboard_no_group),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DropdownArrow()
        }
    }
}

@Composable
private fun DropdownArrow() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(width = ArrowWidth, height = ArrowHeight)) {
        val path =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }
        drawPath(path, color)
    }
}

// TODO: move sign-out to the profile screen once it exists.
@Composable
private fun AccountMenu(
    name: String,
    onSignOutClicked: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val accountLabel = stringResource(R.string.dashboard_account_menu)
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(AccountButtonSize).semantics { contentDescription = accountLabel },
        ) {
            InitialAvatar(name = name)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dashboard_sign_out)) },
                onClick = {
                    expanded = false
                    onSignOutClicked()
                },
            )
        }
    }
}

private val GroupDotSize = 10.dp
private val ArrowWidth = 8.dp
private val ArrowHeight = 5.dp
private val AccountButtonSize = 52.dp

@ComponentPreviews
@Composable
internal fun DashboardHeaderPreview() {
    WissliComponentPreview {
        DashboardHeader(
            greetingName = "Sarah",
            groupName = "Familie Kaufmann",
            onGroupSwitcherClicked = {},
            onSignOutClicked = {},
        )
    }
}

@ComponentPreviews
@Composable
internal fun DashboardHeaderWithoutDataPreview() {
    WissliComponentPreview {
        DashboardHeader(greetingName = null, groupName = null, onGroupSwitcherClicked = {}, onSignOutClicked = {})
    }
}
