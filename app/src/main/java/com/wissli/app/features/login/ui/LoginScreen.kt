package com.wissli.app.features.login.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wissli.app.R
import com.wissli.app.data.auth.LastSignedInAccount
import com.wissli.app.features.login.presentation.LoginEffect
import com.wissli.app.features.login.presentation.LoginError
import com.wissli.app.features.login.presentation.LoginIntent
import com.wissli.app.features.login.presentation.LoginState
import com.wissli.app.features.login.presentation.LoginViewModel
import com.wissli.app.ui.theme.Spacing
import com.wissli.app.ui.theme.WissliTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    val errorMessage = state.error?.let { stringResource(it.messageRes()) }
    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        // Clear only after dismissal; clearing earlier would cancel this effect and hide the snackbar.
        snackbarHostState.showSnackbar(message)
        viewModel.onIntent(LoginIntent.ErrorShown)
    }

    LoginContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onContinueClicked = { viewModel.onIntent(LoginIntent.ContinueWithGoogleClicked(context)) },
        onUseAnotherAccountClicked = { viewModel.onIntent(LoginIntent.UseAnotherAccountClicked(context)) },
    )
}

@Composable
private fun LoginContent(
    state: LoginState,
    snackbarHostState: SnackbarHostState,
    onContinueClicked: () -> Unit,
    onUseAnotherAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // Scrolls on small displays but fills the screen otherwise, keeping the actions at the bottom.
        BoxWithConstraints(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.m),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    WissliWordmark()
                    Spacer(Modifier.height(Spacing.l))
                    QuizCardIllustration(modifier = Modifier.fillMaxWidth())
                }
                // Wait for the last account so the greeting does not switch after appearing.
                AnimatedVisibility(
                    visible = !state.isLoadingLastAccount,
                    enter =
                        fadeIn(tween(ENTER_DURATION_MILLIS)) +
                            slideInVertically(tween(ENTER_DURATION_MILLIS)) { it / ENTER_OFFSET_DIVISOR },
                ) {
                    LoginActions(
                        state = state,
                        onContinueClicked = onContinueClicked,
                        onUseAnotherAccountClicked = onUseAnotherAccountClicked,
                        modifier = Modifier.padding(top = Spacing.xl),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginActions(
    state: LoginState,
    onContinueClicked: () -> Unit,
    onUseAnotherAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastAccount = state.lastAccount
    val greetingName = state.greetingName

    Column(modifier = modifier.fillMaxWidth()) {
        LoginGreeting(isReturning = lastAccount != null, greetingName = greetingName)
        Spacer(Modifier.height(Spacing.l))

        if (lastAccount != null) {
            LastAccountCard(account = lastAccount)
            Spacer(Modifier.height(Spacing.m))
        }

        PrimaryActionButton(
            text =
                when {
                    lastAccount == null -> stringResource(R.string.login_sign_in_with_google)
                    greetingName != null -> stringResource(R.string.login_continue_as, greetingName)
                    else -> stringResource(R.string.login_continue_with_google)
                },
            isLoading = state.isSigningIn,
            onClick = onContinueClicked,
        )

        if (lastAccount != null) {
            Spacer(Modifier.height(Spacing.xs))
            TextButton(
                onClick = onUseAnotherAccountClicked,
                enabled = !state.isSigningIn,
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.xxl),
            ) {
                Text(
                    text = stringResource(R.string.login_use_another_account),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoginGreeting(
    isReturning: Boolean,
    greetingName: String?,
    modifier: Modifier = Modifier,
) {
    // TODO: derive the subtitle from real data (open challenges, new questions) once the
    // repositories exist.
    val title =
        when {
            !isReturning -> stringResource(R.string.login_title_first_time)
            greetingName != null -> stringResource(R.string.login_title_welcome_back_named, greetingName)
            else -> stringResource(R.string.login_title_welcome_back)
        }
    val subtitleRes =
        if (isReturning) R.string.login_subtitle_returning else R.string.login_subtitle_first_time

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(subtitleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val glow = MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = shape,
        modifier =
            modifier
                .fillMaxWidth()
                .height(Spacing.primaryActionHeight)
                .shadow(elevation = Spacing.s, shape = shape, ambientColor = glow, spotColor = glow),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Spacing.l),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@StringRes
private fun LoginError.messageRes(): Int =
    when (this) {
        LoginError.NO_GOOGLE_ACCOUNT -> R.string.login_error_no_google_account
        LoginError.SIGN_IN_FAILED -> R.string.login_error_sign_in_failed
    }

private const val ENTER_DURATION_MILLIS = 450
private const val ENTER_OFFSET_DIVISOR = 6

@Preview(showBackground = true, heightDp = 860)
@Composable
internal fun LoginContentReturningPreview() {
    WissliTheme(darkTheme = false) {
        LoginContent(
            state =
                LoginState(
                    isLoadingLastAccount = false,
                    lastAccount =
                        LastSignedInAccount(displayName = "Sarah Kaufmann", email = "sarah@example.com"),
                ),
            snackbarHostState = remember { SnackbarHostState() },
            onContinueClicked = {},
            onUseAnotherAccountClicked = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
internal fun LoginContentFirstTimePreview() {
    WissliTheme(darkTheme = false) {
        LoginContent(
            state = LoginState(isLoadingLastAccount = false),
            snackbarHostState = remember { SnackbarHostState() },
            onContinueClicked = {},
            onUseAnotherAccountClicked = {},
        )
    }
}
