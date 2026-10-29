package com.wissli.app.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wissli.app.features.dashboard.ui.DashboardScreen
import com.wissli.app.features.login.ui.LoginScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun WissliNavHost(authGateViewModel: AuthGateViewModel = koinViewModel()) {
    val gateState by authGateViewModel.state.collectAsStateWithLifecycle()

    when (gateState) {
        is AuthGateState.Loading -> AuthGateLoading()

        is AuthGateState.LoggedOut ->
            WissliGraph(startDestination = Routes.LOGIN, onSignOut = authGateViewModel::signOut)

        is AuthGateState.LoggedIn ->
            WissliGraph(startDestination = Routes.HOME, onSignOut = authGateViewModel::signOut)
    }
}

@Composable
private fun WissliGraph(
    startDestination: String,
    onSignOut: () -> Unit,
) {
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
            DashboardScreen(
                onSignOut = {
                    onSignOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
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
