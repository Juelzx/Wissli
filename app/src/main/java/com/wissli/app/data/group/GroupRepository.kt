package com.wissli.app.data.group

import com.wissli.app.core.model.Group
import com.wissli.app.core.model.GroupRole
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun observeGroups(): Flow<List<Group>>

    fun observeMembers(groupId: String): Flow<List<GroupMemberSummary>>
}

// TODO: decide where streak and points are stored once challenges are scored.
data class GroupMemberSummary(
    val userId: String,
    val displayName: String,
    val role: GroupRole,
    val streakDays: Int,
    val points: Int,
    val pointsGoal: Int,
    val openChallengeCount: Int,
)
