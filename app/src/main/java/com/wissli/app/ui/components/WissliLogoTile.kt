package com.wissli.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.wissli.app.R
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Spacing

@Composable
fun WissliLogoTile(modifier: Modifier = Modifier) {
    val tileShape = MaterialTheme.shapes.small
    Box(
        modifier =
            modifier
                .size(LogoTileSize)
                .rotate(LOGO_TILT_DEGREES)
                .shadow(elevation = Spacing.xs, shape = tileShape)
                .background(MaterialTheme.colorScheme.primary, tileShape)
                .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.logo_mark),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private val LogoTileSize = 52.dp
private const val LOGO_TILT_DEGREES = -6f

@ComponentPreviews
@Composable
internal fun WissliLogoTilePreview() {
    WissliComponentPreview {
        WissliLogoTile()
    }
}
