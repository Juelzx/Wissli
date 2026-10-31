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
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wissli.app.features.category.ui.CategoryScreen
import com.wissli.app.features.dashboard.ui.DashboardScreen
import com.wissli.app.features.learn.ui.LearnScreen
import com.wissli.app.features.level.ui.LevelScreen
import com.wissli.app.features.login.ui.LoginScreen
import com.wissli.app.features.profile.ui.ProfileScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun WissliNavHost(authGateViewModel: AuthGateViewModel = koinViewModel()) {
    val gateState by authGateViewModel.state.collectAsStateWithLifecycle()

    when (gateState) {
        is AuthGateState.Loading -> AuthGateLoading()

        is AuthGateState.LoggedOut ->
            WissliGraph(startDestination = Route.Login, onSignOut = authGateViewModel::signOut)

        is AuthGateState.LoggedIn ->
            WissliGraph(startDestination = Route.Home, onSignOut = authGateViewModel::signOut)
    }
}

@Composable
private fun WissliGraph(
    startDestination: Route,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    val onTabSelected: (MainTab) -> Unit = navController::navigateToTab
    val onOpenLevel: (String, Int) -> Unit = { categoryId, level ->
        navController.navigate(Route.Level(categoryId, level))
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Login> {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Route.Home) {
                        popUpTo<Route.Login> { inclusive = true }
                    }
                },
            )
        }
        composable<Route.Home> {
            DashboardScreen(
                onOpenProfile = { navController.navigate(Route.Profile) },
                onTabSelected = onTabSelected,
                onOpenLevel = onOpenLevel,
            )
        }
        composable<Route.Profile> {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    onSignOut()
                    // Home sits below the profile, so this clears the whole signed-in stack.
                    navController.navigate(Route.Login) {
                        popUpTo<Route.Home> { inclusive = true }
                    }
                },
            )
        }
        learnDestinations(navController = navController, onTabSelected = onTabSelected, onOpenLevel = onOpenLevel)
    }
}

private fun NavGraphBuilder.learnDestinations(
    navController: NavController,
    onTabSelected: (MainTab) -> Unit,
    onOpenLevel: (categoryId: String, level: Int) -> Unit,
) {
    composable<Route.Learn> {
        LearnScreen(
            onTabSelected = onTabSelected,
            onOpenCategory = { categoryId -> navController.navigate(Route.Category(categoryId)) },
            onOpenLevel = onOpenLevel,
        )
    }
    composable<Route.Category> { entry ->
        CategoryScreen(
            categoryId = entry.toRoute<Route.Category>().categoryId,
            onBack = { navController.popBackStack() },
            onOpenLevel = onOpenLevel,
        )
    }
    composable<Route.Level> { entry ->
        val route = entry.toRoute<Route.Level>()
        LevelScreen(
            categoryId = route.categoryId,
            level = route.level,
            onClose = { navController.popBackStack() },
            // Replaces the finished level, so "back" from the next level leads to where the run started.
            onOpenLevel = { nextCategoryId, nextLevel ->
                navController.navigate(Route.Level(nextCategoryId, nextLevel)) {
                    popUpTo<Route.Level> { inclusive = true }
                }
            },
        )
    }
}

// Home stays at the bottom of the stack, so back from any tab ends on Home before leaving the app.
private fun NavController.navigateToTab(tab: MainTab) {
    val route: Route =
        when (tab) {
            MainTab.HOME -> Route.Home
            MainTab.LEARN -> Route.Learn
            // TODO: add the groups screen (incl. group mode).
            MainTab.GROUPS -> return
        }
    navigate(route) {
        popUpTo<Route.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
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
