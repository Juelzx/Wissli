package com.wissli.app.core.navigation

/**
 * Zentrale Routen-Definition für Navigation Compose. Screens referenzieren ausschließlich diese
 * Konstanten statt String-Literale zu streuen (siehe CLAUDE.md "Navigation").
 *
 * Bewusst simple String-Routen (kein NavType-safe Navigation via kotlinx.serialization), da dafür
 * noch keine zusätzliche Dependency im Version Catalog steht. Bei Bedarf (z. B. sobald Detail-Routen
 * mit Argumenten wie QuestionDetail(questionId) dazukommen) auf typisierte Routen umstellen:
 * kotlinx-serialization-Plugin + `androidx.navigation:navigation-compose` typisierte API.
 *
 * TODO sobald weitere Screens entstehen:
 *  - Main-Graph ergänzen (Home, Questions, Favorites, Challenges, Family, Profile)
 *  - Detail-Routen mit Argumenten (z. B. QuestionDetail, ChallengeDetail)
 *  - Wenn der Graph wächst, auf verschachtelte NavGraphBuilder.navigation(...)-Graphen
 *    (Auth-Graph / Main-Graph) umstellen statt eines einzigen flachen NavHost.
 */
object Routes {
    const val LOGIN = "login"

    // Reiner Platzhalter, bis das echte Home-Feature ansteht — dient aktuell nur als sichtbares
    // Ziel für den Login-Flow und für den Start-Destination-Wechsel in WissliNavHost/AuthGateViewModel.
    const val HOME = "home"
}
