package com.wissli.app.core.navigation

// TODO: switch to type-safe routes (kotlinx.serialization) once the plugin is set up.
object Routes {
    const val LOGIN = "login"

    const val HOME = "home"

    const val LEARN = "learn"

    const val ARG_CATEGORY_ID = "categoryId"
    const val ARG_LEVEL = "level"

    const val CATEGORY = "category/{$ARG_CATEGORY_ID}"
    const val LEVEL = "level/{$ARG_CATEGORY_ID}/{$ARG_LEVEL}"

    fun category(categoryId: String): String = "category/$categoryId"

    fun level(
        categoryId: String,
        level: Int,
    ): String = "level/$categoryId/$level"
}
