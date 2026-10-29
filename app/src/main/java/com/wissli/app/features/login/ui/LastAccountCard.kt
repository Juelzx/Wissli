package com.wissli.app.features.login.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.data.auth.LastSignedInAccount
import com.wissli.app.ui.components.InitialAvatar
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.GoogleBadgeContainer
import com.wissli.app.ui.theme.OnGoogleBadgeContainer
import com.wissli.app.ui.theme.Spacing

@Composable
internal fun LastAccountCard(
    account: LastSignedInAccount,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            InitialAvatar(name = account.displayName.ifBlank { account.email })
            Column(modifier = Modifier.weight(1f)) {
                if (account.displayName.isNotBlank()) {
                    Text(
                        text = account.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (account.email.isNotBlank()) {
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ProviderBadge()
        }
    }
}

@Composable
private fun ProviderBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.login_account_provider_google),
        style = MaterialTheme.typography.labelSmall,
        color = OnGoogleBadgeContainer,
        modifier =
            modifier
                .background(GoogleBadgeContainer, MaterialTheme.shapes.extraSmall)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
    )
}

@ComponentPreviews
@Composable
internal fun LastAccountCardPreview() {
    WissliComponentPreview {
        LastAccountCard(account = LastSignedInAccount(displayName = "Sarah Kaufmann", email = "sarah@example.com"))
    }
}

@ComponentPreviews
@Composable
internal fun LastAccountCardEmailOnlyPreview() {
    WissliComponentPreview {
        LastAccountCard(account = LastSignedInAccount(displayName = "", email = "sarah@example.com"))
    }
}
