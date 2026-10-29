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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wissli.app.features.category.ui.CategoryScreen
import com.wissli.app.features.dashboard.ui.DashboardScreen
import com.wissli.app.features.learn.ui.LearnScreen
import com.wissli.app.features.level.ui.LevelScreen
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
    val onTabSelected: (MainTab) -> Unit = navController::navigateToTab
    val onOpenLevel: (String, Int) -> Unit = { categoryId, level ->
        navController.navigate(Routes.level(categoryId, level))
    }
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
                onTabSelected = onTabSelected,
                onOpenLevel = onOpenLevel,
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
    composable(Routes.LEARN) {
        LearnScreen(
            onTabSelected = onTabSelected,
            onOpenCategory = { categoryId -> navController.navigate(Routes.category(categoryId)) },
            onOpenLevel = onOpenLevel,
        )
    }
    composable(
        route = Routes.CATEGORY,
        arguments = listOf(navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.StringType }),
    ) { entry ->
        CategoryScreen(
            categoryId = entry.arguments?.getString(Routes.ARG_CATEGORY_ID).orEmpty(),
            onBack = { navController.popBackStack() },
            onOpenLevel = onOpenLevel,
        )
    }
    composable(
        route = Routes.LEVEL,
        arguments =
            listOf(
                navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_LEVEL) { type = NavType.IntType },
            ),
    ) { entry ->
        val categoryId = entry.arguments?.getString(Routes.ARG_CATEGORY_ID).orEmpty()
        LevelScreen(
            categoryId = categoryId,
            level = entry.arguments?.getInt(Routes.ARG_LEVEL) ?: 1,
            onClose = { navController.popBackStack() },
            // Replaces the finished level, so "back" from the next level leads to where the run started.
            onOpenLevel = { nextCategoryId, nextLevel ->
                navController.navigate(Routes.level(nextCategoryId, nextLevel)) {
                    popUpTo(Routes.LEVEL) { inclusive = true }
                }
            },
        )
    }
}

// Home stays at the bottom of the stack, so back from any tab ends on Home before leaving the app.
private fun NavController.navigateToTab(tab: MainTab) {
    val route =
        when (tab) {
            MainTab.HOME -> Routes.HOME
            MainTab.LEARN -> Routes.LEARN
            // TODO: add the groups screen (incl. group mode).
            MainTab.GROUPS -> return
        }
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
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
