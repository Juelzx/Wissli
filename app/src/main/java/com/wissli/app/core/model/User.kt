package com.wissli.app.core.model

/** Deliberately holds no personal data beyond [displayName]; roles live on [GroupMember]. */
data class User(
    val id: String,
    val displayName: String,
    val createdAt: Long,
)
