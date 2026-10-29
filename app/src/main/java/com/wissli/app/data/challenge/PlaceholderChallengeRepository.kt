package com.wissli.app.data.challenge

import com.wissli.app.core.model.ChallengeStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit

// TODO: replace with a Firestore implementation (challenges assigned to the current user).
class PlaceholderChallengeRepository : ChallengeRepository {
    override fun observeOpenChallenges(groupId: String): Flow<List<ChallengeSummary>> =
        flow {
            val now = System.currentTimeMillis()
            emit(
                listOf(
                    ChallengeSummary(
                        id = "zebra-stripes",
                        questionTitle = "Warum haben Zebras Streifen?",
                        createdByName = "Mila",
                        createdAt = now - TimeUnit.HOURS.toMillis(2),
                        status = ChallengeStatus.PENDING,
                    ),
                    ChallengeSummary(
                        id = "moon-shape",
                        questionTitle = "Warum ändert der Mond seine Form?",
                        createdByName = "Ben",
                        createdAt = now - TimeUnit.DAYS.toMillis(1),
                        status = ChallengeStatus.IN_PROGRESS,
                    ),
                ),
            )
        }
}
