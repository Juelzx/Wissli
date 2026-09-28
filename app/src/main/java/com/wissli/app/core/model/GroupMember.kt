package com.wissli.app.core.model

/**
 * Mitgliedschaft eines [User] in einer [Group] — trägt die Rolle *pro Gruppe* statt global auf
 * dem User (ein User kann in Gruppe A Owner und in Gruppe B einfaches Mitglied sein). [OWNER] ist
 * automatisch, wer die Gruppe erstellt hat, und ist die einzige Rolle, die Mitglieder
 * einladen/entfernen oder die Gruppe verwalten darf (siehe CLAUDE.md "Firebase & Firestore").
 */
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
