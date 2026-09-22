package com.wissli.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wissli.app.feature.auth.login.LoginScreen

/**
 * Wurzel-NavHost. Aktuell nur der Login-Screen — sobald ein Main-Graph existiert (Home etc.),
 * hier den Auth-Graph und Main-Graph als zwei verschachtelte navigation(...)-Graphen aufteilen und
 * den Start-Destination-Wechsel anhand von AuthRepository.observeCurrentUser() steuern (z. B. über
 * einen kleinen Root-ViewModel, der den Login-Status beobachtet).
 */
@Composable
fun WissliNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToHome = {
                    // TODO: sobald Routes.HOME existiert: navController.navigate(Routes.HOME) {
                    //     popUpTo(Routes.LOGIN) { inclusive = true }
                    // }
                },
            )
        }
    }
}
