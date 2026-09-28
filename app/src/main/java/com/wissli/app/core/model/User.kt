package com.wissli.app.core.model

/**
 * Domain-Modell, siehe CLAUDE.md ("Domain-Modelle (Kern)"). Trägt bewusst keine Rolle und keine
 * Gruppenzugehörigkeit mehr — beides lebt jetzt pro Mitgliedschaft in [GroupMember], da ein User
 * Mitglied mehrerer Gruppen sein kann (siehe [Group]). So wenig personenbezogene Daten wie
 * möglich über [displayName] hinaus.
 */
data class User(
    val id: String,
    val displayName: String,
    val createdAt: Long,
)
