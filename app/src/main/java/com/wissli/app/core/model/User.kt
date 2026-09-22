package com.wissli.app.core.model

/**
 * Domain-Modell, siehe CLAUDE.md ("Domain-Modelle (Kern)"). Bewusst ohne personenbezogene
 * Felder über [displayName] hinaus — insbesondere für Kinder-Accounts.
 */
data class User(
    val id: String,
    val displayName: String,
    val role: UserRole,
    val familyId: String?,
    val createdAt: Long,
)

enum class UserRole {
    PARENT,
    CHILD,
    ADMIN,
}
