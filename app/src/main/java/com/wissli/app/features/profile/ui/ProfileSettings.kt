package com.wissli.app.features.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.core.model.AppLanguage
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageSetting(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingCard(modifier = modifier) {
        SettingTexts(
            title = stringResource(R.string.profile_language_title),
            subtitle = stringResource(R.string.profile_language_subtitle),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = Spacing.s)) {
            AppLanguage.entries.forEachIndexed { index, language ->
                SegmentedButton(
                    selected = language == selected,
                    onClick = { onSelected(language) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = AppLanguage.entries.size),
                ) {
                    Text(text = stringResource(language.labelRes))
                }
            }
        }
    }
}

@Composable
internal fun NotificationsSetting(
    enabled: Boolean,
    onToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SettingTexts(
                title = stringResource(R.string.profile_notifications_title),
                subtitle = stringResource(R.string.profile_notifications_subtitle),
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onToggled)
        }
    }
}

@Composable
private fun SettingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.m)) {
            content()
        }
    }
}

@Composable
private fun SettingTexts(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val AppLanguage.labelRes: Int
    get() =
        when (this) {
            AppLanguage.GERMAN -> R.string.profile_language_german
            AppLanguage.ENGLISH -> R.string.profile_language_english
        }

@ComponentPreviews
@Composable
internal fun ProfileSettingsPreview() {
    WissliComponentPreview {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            LanguageSetting(selected = AppLanguage.GERMAN, onSelected = {})
            NotificationsSetting(enabled = true, onToggled = {})
        }
    }
}
