package com.wissli.app.core.model

/**
 * A subject such as "Natur". [levelCount] counts the levels that already have published questions;
 * [order] sorts the categories and picks their color.
 */
data class Category(
    val id: String,
    val name: String,
    val order: Int,
    val levelCount: Int,
)
