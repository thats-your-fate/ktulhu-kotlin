package com.ktulhu.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import com.ktulhu.ai.ui.theme.KColors
import com.ktulhu.ai.viewmodel.AuthViewModel
import com.ktulhu.ai.viewmodel.SessionState

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    sessionState: SessionState,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val user by authViewModel.user.collectAsState()
    val c = KColors

    Scaffold(
        containerColor = c.appBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Account Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (user == null) {
                Text("Not authenticated.", color = c.cardSubtitle)
                return@Box
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = c.cardBg,
                    contentColor = c.cardText
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RowItem(
                        icon = Icons.Outlined.Shield,
                        label = "Login Provider",
                        value = user?.provider?.replaceFirstChar { it.uppercase() } ?: "-"
                    )
                    user?.email?.let {
                        RowItem(
                            icon = Icons.Outlined.Mail,
                            label = "Email",
                            value = it
                        )
                    }
                    RowItem(
                        icon = Icons.Outlined.Person,
                        label = "User ID",
                        value = user?.id ?: "-"
                    )
                    RowItem(
                        icon = Icons.Outlined.Smartphone,
                        label = "Device Hash",
                        value = sessionState.deviceHash
                    )

                    user?.jwt?.takeIf { it.isNotBlank() }?.let { token ->
                        Text(
                            text = "JWT",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.cardSubtitle
                        )
                        Text(
                            text = token,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isDark = isSystemInDarkTheme()
                    val textColor = if (isDark) c.btnGhostTextDark else c.btnGhostText
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = textColor
                        )
                    ) {
                        Text("Back to chats")
                    }
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) c.btnDefaultBgDark else c.btnDefaultBg,
                            contentColor = if (isDark) c.btnDefaultTextDark else c.btnDefaultText
                        )
                    ) {
                        Text("Log out")
                    }
                }
            }
        }
    }
}
}

@Composable
private fun RowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    val c = KColors
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = c.cardSubtitle)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = c.cardSubtitle)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = c.cardText)
        }
    }
}
