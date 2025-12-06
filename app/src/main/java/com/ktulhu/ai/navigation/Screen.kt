package com.ktulhu.ai.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object EmailLogin : Screen("email_login")
    data object Main : Screen("main")
    data object Chat : Screen("chat")
}
