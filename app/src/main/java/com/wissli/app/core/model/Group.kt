package com.wissli.app.core.model

/**
 * Domain-Modell, siehe CLAUDE.md ("Domain-Modelle (Kern)"). Ersetzt das frühere starre
 * Family-Konzept (Parent → Child): eine Gruppe hat keinen festen Typ — sie kann eine Familie
 * sein ("Familie Scholler"), ein Freundeskreis oder jede andere Konstellation. Jede:r User kann
 * beliebig viele Gruppen erstellen und Mitglied mehrerer Gruppen sein (siehe [GroupMember]).
 */
data class Group(
    val id: String,
    val name: String,
    val createdBy: String,
    val createdAt: Long,
)
