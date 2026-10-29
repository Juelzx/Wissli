package com.wissli.app.core.model

/**
 * [createdBy] never changes, while [ownerId] moves with an ownership transfer. Members live in the
 * groups/{groupId}/members subcollection, not on the group.
 */
data class Group(
    val id: String,
    val name: String,
    val createdBy: String,
    val ownerId: String,
    val createdAt: Long,
)
