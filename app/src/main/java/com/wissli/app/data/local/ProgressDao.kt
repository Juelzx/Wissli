package com.wissli.app.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE userId = :userId")
    fun observeAll(userId: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE userId = :userId")
    suspend fun getAll(userId: String): List<ProgressEntity>

    @Query("SELECT * FROM progress WHERE userId = :userId AND categoryId = :categoryId")
    suspend fun get(
        userId: String,
        categoryId: String,
    ): ProgressEntity?

    @Upsert
    suspend fun upsert(entity: ProgressEntity)

    /** Only clears the flag if nothing was solved since [updatedAt] was uploaded. */
    @Query(
        """
        UPDATE progress SET pendingSync = 0
        WHERE userId = :userId AND categoryId = :categoryId AND updatedAt = :updatedAt
        """,
    )
    suspend fun markSynced(
        userId: String,
        categoryId: String,
        updatedAt: Long,
    )
}
