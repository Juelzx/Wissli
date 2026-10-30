package com.wissli.app.data.local

import androidx.room3.Entity

/**
 * Solo progress per account and category. Rows are kept per [userId], so switching accounts on one
 * device never mixes progress. [pendingSync] stays set until Firestore has confirmed the row.
 */
@Entity(tableName = "progress", primaryKeys = ["userId", "categoryId"])
data class ProgressEntity(
    val userId: String,
    val categoryId: String,
    val completedLevels: Int,
    val solvedJson: String,
    val updatedAt: Long,
    val pendingSync: Boolean,
)
