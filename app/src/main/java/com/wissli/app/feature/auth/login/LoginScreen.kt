package com.wissli.app.feature.auth.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    // TODO: Toast durch Scaffold(snackbarHost = ...) + SnackbarHostState ersetzen, sobald das
    // Theme/Design-System dafür steht — passt besser zum angestrebten Material-3-Look.
    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.onIntent(LoginIntent.ErrorMessageShown)
    }

    LoginContent(
        isLoading = state.isLoading,
        onSignInClicked = { viewModel.onIntent(LoginIntent.SignInWithGoogleClicked(context)) },
    )
}

@Composable
private fun LoginContent(
    isLoading: Boolean,
    onSignInClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Willkommen bei Wissli", style = MaterialTheme.typography.headlineMedium)

            // TODO: durch offiziellen Google-Sign-In-Button/Branding ersetzen statt Standard-Button —
            // siehe https://developers.google.com/identity/branding-guidelines
            Button(onClick = onSignInClicked, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Mit Google anmelden")
                }
            }
        }
    }
}
