package com.wissli.app.core.navigation

import kotlinx.serialization.Serializable

/** Navigation destinations; Navigation Compose derives route patterns and typed arguments from these. */
sealed interface Route {
    @Serializable
    data object Login : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Learn : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data class Category(
        val categoryId: String,
    ) : Route

    @Serializable
    data class Level(
        val categoryId: String,
        val level: Int,
    ) : Route
}
