package com.wissli.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview

/** Rounded bar whose fill eases to [progress] (0..1), so every solved question visibly moves it. */
@Composable
fun WissliProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = DefaultHeight,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = FILL_ANIMATION_MILLIS),
    )
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .background(trackColor, CircleShape),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .background(fillColor, CircleShape),
        )
    }
}

private val DefaultHeight = 10.dp
private const val FILL_ANIMATION_MILLIS = 600

@ComponentPreviews
@Composable
internal fun WissliProgressBarPreview() {
    WissliComponentPreview {
        WissliProgressBar(progress = 0.6f)
    }
}
