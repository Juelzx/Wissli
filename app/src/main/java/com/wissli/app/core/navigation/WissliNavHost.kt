package com.wissli.app.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wissli.app.feature.auth.login.LoginScreen
import org.koin.androidx.compose.koinViewModel

/**
 * Wurzel-NavHost. Wartet zunächst auf den initialen Login-Status ([AuthGateViewModel]), damit
 * bereits angemeldete Nutzer:innen beim App-Start nicht erneut den Login-Screen sehen, statt
 * hart bei [Routes.LOGIN] zu starten.
 */
@Composable
fun WissliNavHost(authGateViewModel: AuthGateViewModel = koinViewModel()) {
    val gateState by authGateViewModel.state.collectAsState()

    when (gateState) {
        AuthGateState.Loading -> AuthGateLoading()
        AuthGateState.LoggedOut -> WissliGraph(startDestination = Routes.LOGIN)
        is AuthGateState.LoggedIn -> WissliGraph(startDestination = Routes.HOME)
    }
}

@Composable
private fun WissliGraph(startDestination: String) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            // TODO: durch den echten Home-Screen ersetzen, sobald das Feature ansteht (siehe
            // CLAUDE.md "Entwicklungsstrategie") — reiner Platzhalter, damit der Login-Flow ein
            // sichtbares Ziel hat.
            HomePlaceholder()
        }
    }
}

@Composable
private fun AuthGateLoading() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun HomePlaceholder() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Angemeldet ✓", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
