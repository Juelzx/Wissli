package com.wissli.app.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wissli.app.ui.theme.Spacing
import com.wissli.app.ui.theme.WissliTheme

@Preview(name = "Light", device = PHONE_DEVICE)
@Preview(name = "Dark", device = PHONE_DEVICE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "German", device = PHONE_DEVICE, locale = "de")
annotation class ScreenPreviews

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ComponentPreviews

// Previews render without a window background, so dark mode needs an explicit surface to be readable.
@Composable
internal fun WissliComponentPreview(content: @Composable () -> Unit) {
    WissliTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(Spacing.m)) {
                content()
            }
        }
    }
}

private const val PHONE_DEVICE = "spec:width=411dp,height=891dp"
