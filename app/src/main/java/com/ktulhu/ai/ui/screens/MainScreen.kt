// com/ktulhu/ai/ui/screens/MainScreen.kt
package com.ktulhu.ai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // TODO: plug in your existing “main logic” UI here.
    // If you send me your current Compose/React layout, I can map it.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ktulhu Ai – Anonymous Session") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Here goes your main chat / logic UI.")
        }
    }
}
