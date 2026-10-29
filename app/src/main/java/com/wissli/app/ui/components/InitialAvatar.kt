package com.wissli.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.LeafContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.Spacing

// Initial instead of a profile photo: no image loader dependency and no extra personal data.
@Composable
fun InitialAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = DefaultAvatarSize,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
        )
    }
}

private val DefaultAvatarSize = 48.dp

@ComponentPreviews
@Composable
internal fun InitialAvatarPreview() {
    WissliComponentPreview {
        Row {
            InitialAvatar(name = "Sarah")
            InitialAvatar(
                name = "Mila",
                containerColor = LeafContainer,
                contentColor = OnLeafContainer,
                modifier = Modifier.padding(start = Spacing.s),
            )
        }
    }
}
