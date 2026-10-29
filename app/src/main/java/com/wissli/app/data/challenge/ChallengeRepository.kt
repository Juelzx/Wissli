package com.wissli.app.data.challenge

import com.wissli.app.core.model.ChallengeStatus
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {
    fun observeOpenChallenges(groupId: String): Flow<List<ChallengeSummary>>
}

data class ChallengeSummary(
    val id: String,
    val questionTitle: String,
    val createdByName: String,
    val createdAt: Long,
    val status: ChallengeStatus,
)
