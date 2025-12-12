package com.ktulhu.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.ktulhu.ai.data.ServiceLocator
import com.ktulhu.ai.navigation.KtulhuNavHost
import com.ktulhu.ai.ui.theme.KtulhuTheme
import com.ktulhu.ai.viewmodel.SessionViewModel


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Correct edge-to-edge call for your dependency versions
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.White.copy(alpha = 0.2f).toArgb(),
                darkScrim = Color.Black.copy(alpha = 0.2f).toArgb()
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.White.copy(alpha = 0.2f).toArgb(),
                darkScrim = Color.Black.copy(alpha = 0.2f).toArgb()
            )
        )

        setContent {
            KtulhuTheme {
                val navController = rememberNavController()
                val sessionViewModel: SessionViewModel = viewModel()
                val session by sessionViewModel.state.collectAsState()

                LaunchedEffect(session) {
                    if (!ServiceLocator.usingStubData) {
                        ServiceLocator.socketManager.ensureConnected(session)
                    }
                }

                // Root MUST stay transparent
                Surface(color = Color.Transparent) {
                    KtulhuNavHost(
                        navController = navController,
                        sessionViewModel = sessionViewModel
                    )
                }
            }
        }
    }
}
