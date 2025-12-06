package com.ktulhu.ai

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.ktulhu.ai.data.ServiceLocator
import com.ktulhu.ai.navigation.KtulhuNavHost
import com.ktulhu.ai.ui.theme.KColors
import com.ktulhu.ai.ui.theme.KtulhuTheme
import com.ktulhu.ai.viewmodel.SessionViewModel
import androidx.compose.ui.graphics.luminance

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            // ---------- App Theme Wrapper ----------
            KtulhuTheme {

                // Access semantic colors (works only inside theme)
                val c = KColors

                val view = LocalView.current

                // System bars follow appBg semantic color
                val systemBarColor = c.appBg

                SideEffect {
                    val window = (view.context as Activity).window

                    window.statusBarColor = systemBarColor.toArgb()
                    window.navigationBarColor = systemBarColor.toArgb()

                    val insets = WindowCompat.getInsetsController(window, window.decorView)
                    val isLight = systemBarColor.luminance() > 0.5f

                    insets.isAppearanceLightStatusBars = isLight
                    insets.isAppearanceLightNavigationBars = isLight
                }

                // ---------- App Root Surface ----------
                Surface(color = c.appBg) {

                    val navController = rememberNavController()
                    val sessionViewModel: SessionViewModel = viewModel()
                    val session by sessionViewModel.state.collectAsState()

                    // WebSocket connect if not stub mode
                    LaunchedEffect(session) {
                        if (!ServiceLocator.usingStubData) {
                            ServiceLocator.socketManager.ensureConnected(session)
                        }
                    }

                    KtulhuNavHost(
                        navController = navController,
                        sessionViewModel = sessionViewModel
                    )
                }
            }
        }
    }
}
