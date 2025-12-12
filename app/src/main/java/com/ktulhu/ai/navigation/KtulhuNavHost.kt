package com.ktulhu.ai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ktulhu.ai.ui.screens.AuthScreen
import com.ktulhu.ai.ui.screens.ChatShellScreen
import com.ktulhu.ai.ui.screens.EmailLoginScreen
import com.ktulhu.ai.ui.screens.EmailRegisterScreen
import com.ktulhu.ai.viewmodel.AuthViewModel
import com.ktulhu.ai.viewmodel.SessionViewModel

@Composable
fun KtulhuNavHost(
    navController: NavHostController,
    sessionViewModel: SessionViewModel
) {
    val authViewModel: AuthViewModel = viewModel()
    val sessionState by sessionViewModel.state.collectAsState()
    authViewModel.deviceHash = sessionState.deviceHash

    val navigateToMain: () -> Unit = {
        sessionViewModel.newChat()
        navController.navigate(Screen.Main.route) {
            popUpTo(Screen.Auth.route) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                vm = authViewModel,
                onEmailClick = { navController.navigate(Screen.EmailLogin.route) },
                onComplete = navigateToMain
            )
        }

        composable(Screen.EmailLogin.route) {
            EmailLoginScreen(
                vm = authViewModel,
                onBack = { navController.popBackStack() },
                onRegisterClick = { navController.navigate(Screen.EmailRegister.route) },
                onComplete = navigateToMain
            )
        }

        composable(Screen.EmailRegister.route) {
            EmailRegisterScreen(
                vm = authViewModel,
                onBack = { navController.popBackStack() },
                onLoginClick = { navController.popBackStack() },
                onComplete = navigateToMain
            )
        }

        composable(Screen.Main.route) {
            ChatShellScreen(sessionViewModel = sessionViewModel)
        }
    }
}
