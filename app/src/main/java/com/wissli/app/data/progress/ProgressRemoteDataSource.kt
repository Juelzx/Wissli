package com.wissli.app.data.progress

import com.wissli.app.core.model.CategoryProgress

/** Remote copy of the solo progress, one document per category at users/{uid}/progress/{categoryId}. */
interface ProgressRemoteDataSource {
    /** Reads from the server only and fails while offline, so a stale cache is never merged as truth. */
    suspend fun fetchAll(userId: String): List<CategoryProgress>

    /** Returns once the server has confirmed the write. */
    suspend fun save(
        userId: String,
        progress: CategoryProgress,
    )
}
