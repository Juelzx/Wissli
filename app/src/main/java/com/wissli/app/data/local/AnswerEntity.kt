package com.wissli.app.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * One answer given in a level, right or wrong. Kept only on the device for the learning overview;
 * [epochDay] is the local calendar day at the time of the answer, so days never shift with time zones.
 */
@Entity(tableName = "answers", indices = [Index(value = ["userId"])])
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val categoryId: String,
    val questionId: String,
    val correct: Boolean,
    val answeredAt: Long,
    val epochDay: Long,
)

data class CategoryAnswerCount(
    val categoryId: String,
    val answered: Int,
    val correct: Int,
)

data class DayAnswerCount(
    val epochDay: Long,
    val answered: Int,
)
