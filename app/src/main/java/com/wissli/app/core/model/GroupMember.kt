package com.wissli.app.core.model

/** The role lives on the membership, so a user can be OWNER in one group and MEMBER in another. */
enum class GroupRole {
    OWNER,
    MEMBER,
}

data class GroupMember(
    val userId: String,
    val groupId: String,
    val role: GroupRole,
    val joinedAt: Long,
)
