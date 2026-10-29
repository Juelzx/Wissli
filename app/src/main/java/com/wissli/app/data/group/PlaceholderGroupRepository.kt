package com.wissli.app.data.group

import com.wissli.app.core.model.Group
import com.wissli.app.core.model.GroupRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// TODO: replace with a Firestore implementation (groups + members subcollection).
class PlaceholderGroupRepository : GroupRepository {
    override fun observeGroups(): Flow<List<Group>> =
        flowOf(
            listOf(
                Group(
                    id = SAMPLE_GROUP_ID,
                    name = "Familie Kaufmann",
                    createdBy = "sarah",
                    ownerId = "sarah",
                    createdAt = 0L,
                ),
            ),
        )

    override fun observeMembers(groupId: String): Flow<List<GroupMemberSummary>> =
        flowOf(
            listOf(
                GroupMemberSummary(
                    userId = "jonas",
                    displayName = "Jonas",
                    role = GroupRole.OWNER,
                    streakDays = 2,
                    points = 60,
                    pointsGoal = 150,
                ),
                GroupMemberSummary(
                    userId = "mila",
                    displayName = "Mila",
                    role = GroupRole.MEMBER,
                    streakDays = 4,
                    points = 120,
                    pointsGoal = 150,
                ),
                GroupMemberSummary(
                    userId = "ben",
                    displayName = "Ben",
                    role = GroupRole.MEMBER,
                    streakDays = 0,
                    points = 40,
                    pointsGoal = 150,
                ),
            ),
        )

    private companion object {
        const val SAMPLE_GROUP_ID = "sample-group"
    }
}
