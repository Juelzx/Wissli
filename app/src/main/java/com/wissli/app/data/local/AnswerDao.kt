package com.wissli.app.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {
    @Insert
    suspend fun insert(answer: AnswerEntity)

    @Query(
        """
        SELECT categoryId, COUNT(*) AS answered, SUM(correct) AS correct
        FROM answers WHERE userId = :userId
        GROUP BY categoryId
        """,
    )
    fun observeCategoryCounts(userId: String): Flow<List<CategoryAnswerCount>>

    /** The [limit] most recent days with answers. */
    @Query(
        """
        SELECT epochDay, COUNT(*) AS answered
        FROM answers WHERE userId = :userId
        GROUP BY epochDay ORDER BY epochDay DESC LIMIT :limit
        """,
    )
    fun observeDayCounts(
        userId: String,
        limit: Int,
    ): Flow<List<DayAnswerCount>>
}
