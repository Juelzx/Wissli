package com.wissli.app.ui.components

import androidx.compose.ui.graphics.Color
import com.wissli.app.ui.theme.Blush
import com.wissli.app.ui.theme.Lilac
import com.wissli.app.ui.theme.Mint
import com.wissli.app.ui.theme.OnCoralContainer
import com.wissli.app.ui.theme.OnLavenderContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.OnPeachContainer
import com.wissli.app.ui.theme.OnSkyContainer
import com.wissli.app.ui.theme.Peach
import com.wissli.app.ui.theme.Sky

data class CategoryColors(
    val container: Color,
    val accent: Color,
)

/** Derived from [com.wissli.app.core.model.Category.order], so a category keeps its color on every screen. */
fun categoryColors(order: Int): CategoryColors = CategoryPalette[Math.floorMod(order, CategoryPalette.size)]

private val CategoryPalette =
    listOf(
        CategoryColors(container = Mint, accent = OnLeafContainer),
        CategoryColors(container = Sky, accent = OnSkyContainer),
        CategoryColors(container = Peach, accent = OnPeachContainer),
        CategoryColors(container = Blush, accent = OnCoralContainer),
        CategoryColors(container = Lilac, accent = OnLavenderContainer),
    )
