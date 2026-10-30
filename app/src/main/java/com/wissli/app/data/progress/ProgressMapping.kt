package com.wissli.app.data.progress

import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.data.local.ProgressEntity
import org.json.JSONArray

internal fun ProgressEntity.toModel(): CategoryProgress {
    val solved = JSONArray(solvedJson)
    return CategoryProgress(
        categoryId = categoryId,
        completedLevels = completedLevels,
        solvedInCurrentLevel = (0 until solved.length()).map(solved::getString).toSet(),
        updatedAt = updatedAt,
    )
}

internal fun CategoryProgress.toEntity(
    userId: String,
    pendingSync: Boolean,
) = ProgressEntity(
    userId = userId,
    categoryId = categoryId,
    completedLevels = completedLevels,
    solvedJson = JSONArray(solvedInCurrentLevel.sorted()).toString(),
    updatedAt = updatedAt,
    pendingSync = pendingSync,
)
