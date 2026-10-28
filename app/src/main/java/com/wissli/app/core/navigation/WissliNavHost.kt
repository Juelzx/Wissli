package com.wissli.app.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wissli.app.R
import com.wissli.app.features.login.ui.LoginScreen
import com.wissli.app.ui.theme.Spacing
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
            // TODO: replace with the real home screen.
            HomePlaceholder(
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

@Composable
private fun HomePlaceholder(onSignOut: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_placeholder_signed_in),
                style = MaterialTheme.typography.headlineMedium,
            )
            OutlinedButton(onClick = onSignOut) {
                Text(text = stringResource(R.string.home_placeholder_sign_out))
            }
        }
    }
}
