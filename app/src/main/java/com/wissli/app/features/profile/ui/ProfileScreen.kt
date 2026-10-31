package com.wissli.app.features.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wissli.app.R
import com.wissli.app.core.model.AppLanguage
import com.wissli.app.features.profile.presentation.ProfileIntent
import com.wissli.app.features.profile.presentation.ProfileState
import com.wissli.app.features.profile.presentation.ProfileViewModel
import com.wissli.app.ui.components.InitialAvatar
import com.wissli.app.ui.preview.ScreenPreviews
import com.wissli.app.ui.theme.Spacing
import com.wissli.app.ui.theme.WissliTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileContent(state = state, onIntent = viewModel::onIntent, onBack = onBack, onSignOut = onSignOut)
}

@Composable
private fun ProfileContent(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.profile_back), style = MaterialTheme.typography.labelLarge)
            }
            ProfileHeader(displayName = state.displayName)
            Text(
                text = stringResource(R.string.profile_settings_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.s),
            )
            LanguageSetting(
                selected = state.language,
                onSelected = { onIntent(ProfileIntent.LanguageSelected(it)) },
            )
            NotificationsSetting(
                enabled = state.notificationsEnabled,
                onToggled = { onIntent(ProfileIntent.NotificationsToggled(it)) },
            )
            OutlinedButton(
                onClick = onSignOut,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.l).heightIn(min = SignOutHeight),
            ) {
                Text(text = stringResource(R.string.profile_sign_out), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun ProfileHeader(displayName: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        InitialAvatar(name = displayName.orEmpty(), size = AvatarSize)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = displayName ?: stringResource(R.string.profile_no_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val AvatarSize = 72.dp
private val SignOutHeight = 56.dp

@ScreenPreviews
@Composable
internal fun ProfilePreview() {
    WissliTheme {
        ProfileContent(
            state = ProfileState(displayName = "Sarah Kaufmann", language = AppLanguage.GERMAN),
            onIntent = {},
            onBack = {},
            onSignOut = {},
        )
    }
}
