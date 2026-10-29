package com.wissli.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Blush
import com.wissli.app.ui.theme.Mint
import com.wissli.app.ui.theme.Spacing

/**
 * Wissli's signature card look: [frontCard] on top of two tilted, colored cards. The padding leaves
 * room for the tilted cards, and [fanOut] (0 = stacked, 1 = fanned out) lets callers animate them.
 */
@Composable
fun CardStack(
    modifier: Modifier = Modifier,
    fanOut: () -> Float = { 1f },
    frontCard: @Composable () -> Unit,
) {
    Box(modifier = modifier.padding(start = Spacing.m, end = Spacing.m, top = Spacing.xxs, bottom = Spacing.xl)) {
        BackCard(color = Mint, rotation = 7f, offsetX = 14.dp, offsetY = 26.dp, fanOut = fanOut)
        BackCard(color = Blush, rotation = -5f, offsetX = (-10).dp, offsetY = 30.dp, fanOut = fanOut)
        frontCard()
    }
}

@Composable
private fun BoxScope.BackCard(
    color: Color,
    rotation: Float,
    offsetX: Dp,
    offsetY: Dp,
    fanOut: () -> Float,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val p = fanOut()
                    rotationZ = rotation * p
                    translationX = offsetX.toPx() * p
                    translationY = offsetY.toPx() * p
                }.background(color, shape),
    )
}

@ComponentPreviews
@Composable
internal fun CardStackPreview() {
    WissliComponentPreview {
        CardStack {
            Surface(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = Spacing.xs,
            ) {}
        }
    }
}
