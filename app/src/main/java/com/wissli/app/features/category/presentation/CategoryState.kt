package com.wissli.app.features.category.presentation

import com.wissli.app.core.model.CategoryOverview

data class CategoryState(
    val isLoading: Boolean = true,
    val overview: CategoryOverview? = null,
)
