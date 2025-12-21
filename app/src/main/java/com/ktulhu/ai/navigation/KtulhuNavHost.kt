package com.ktulhu.ai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ktulhu.ai.ui.screens.ChatShellScreen
import com.ktulhu.ai.ui.screens.SettingsScreen
import com.ktulhu.ai.viewmodel.AuthViewModel
import com.ktulhu.ai.viewmodel.SessionViewModel

@Composable
fun KtulhuNavHost(
    navController: NavHostController,
    sessionViewModel: SessionViewModel
) {
    val authViewModel: AuthViewModel = viewModel()
    val sessionState by sessionViewModel.state.collectAsState()
    val authUser by authViewModel.user.collectAsState()
    authViewModel.deviceHash = sessionState.deviceHash

    LaunchedEffect(authUser) {
        if (authUser == null) {
            authViewModel.loginAnonymous()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            ChatShellScreen(
                sessionViewModel = sessionViewModel,
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                authViewModel = authViewModel,
                sessionState = sessionState,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    authViewModel.loginAnonymous()
                    sessionViewModel.newChat()
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                }
            )
        }
    }
}
