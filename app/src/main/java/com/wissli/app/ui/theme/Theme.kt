package com.wissli.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
    lightColorScheme(
        primary = WissliBlue,
        onPrimary = PaperWhite,
        primaryContainer = WissliBlueContainer,
        onPrimaryContainer = OnWissliBlueContainer,
        secondary = Leaf,
        onSecondary = PaperWhite,
        secondaryContainer = LeafContainer,
        onSecondaryContainer = OnLeafContainer,
        background = Paper,
        onBackground = Ink,
        surface = Paper,
        onSurface = Ink,
        onSurfaceVariant = InkMuted,
        surfaceContainerLowest = PaperWhite,
        surfaceContainerLow = PaperWhite,
        outline = InkMuted,
        outlineVariant = Hairline,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = WissliBlueDark,
        onPrimary = OnWissliBlueDark,
        primaryContainer = WissliBlueContainerDark,
        onPrimaryContainer = OnWissliBlueContainerDark,
        secondary = LeafDark,
        onSecondary = OnLeafDark,
        secondaryContainer = LeafContainerDark,
        onSecondaryContainer = OnLeafContainerDark,
        background = PaperDark,
        onBackground = InkDark,
        surface = PaperDark,
        onSurface = InkDark,
        onSurfaceVariant = InkMutedDark,
        surfaceContainerLowest = SurfaceDark,
        surfaceContainerLow = SurfaceDark,
        outline = InkMutedDark,
        outlineVariant = HairlineDark,
    )

// No dynamic color on purpose, so the brand colors look the same on every device.
@Composable
fun WissliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
