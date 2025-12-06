// com/ktulhu/ai/navigation/KtulhuNavHost.kt
package com.ktulhu.ai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ktulhu.ai.ui.screens.AuthScreen
import com.ktulhu.ai.ui.screens.ChatShellScreen
import com.ktulhu.ai.ui.screens.EmailLoginScreen
import com.ktulhu.ai.viewmodel.SessionViewModel

@Composable
fun KtulhuNavHost(
    navController: NavHostController,
    sessionViewModel: SessionViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAnonymousClick = {
                    sessionViewModel.newChat()
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onEmailClick = {
                    navController.navigate(Screen.EmailLogin.route)
                },
                onGoogleClick = {
                    // TODO: plug in Google Sign-In flow
                },
                onAppleClick = {
                    // TODO: plug in Apple Sign-In flow (web / one-tap)
                }
            )
        }

        composable(Screen.EmailLogin.route) {
            EmailLoginScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                    sessionViewModel.newChat()
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            ChatShellScreen(sessionViewModel = sessionViewModel)
        }
    }
}
