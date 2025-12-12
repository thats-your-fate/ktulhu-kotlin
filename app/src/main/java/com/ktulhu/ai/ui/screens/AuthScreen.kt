package com.ktulhu.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ktulhu.ai.ui.components.AuthButton
import com.ktulhu.ai.ui.components.TypewriterCarousel
import com.ktulhu.ai.ui.theme.KColors
import androidx.compose.ui.res.painterResource
import com.ktulhu.ai.R
import com.ktulhu.ai.viewmodel.AuthViewModel


@Composable
fun AuthScreen(
    vm: AuthViewModel,
    onEmailClick: () -> Unit,
    onComplete: () -> Unit = {}
) {
    val user by vm.user.collectAsState()
    val c = KColors
    val heroMessages = listOf(
        stringResource(R.string.hero_message_workspace),
        stringResource(R.string.hero_message_brainstorm),
        stringResource(R.string.hero_message_secure)
    )

    LaunchedEffect(user) {
        if (user != null) {
            onComplete()
        }
    }

    if (user != null) return

    Surface(
        color = c.appBg,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = stringResource(R.string.app_name),
                    color = c.headerTitle,
                    style = MaterialTheme.typography.headlineLarge
                )

                TypewriterCarousel(
                    messages = heroMessages,
                    modifier = Modifier.fillMaxWidth(),
                    typingSpeedMillis = 55,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                AuthButton(
                    text = stringResource(R.string.auth_continue_google),
                    icon = painterResource(R.drawable.ic_google_colored),
                    onClick = { vm.loginGoogle("<google-id-token>") }
                )

                AuthButton(
                    text = stringResource(R.string.auth_continue_apple),
                    icon = painterResource(R.drawable.ic_apple_filled),
                    onClick = { vm.loginApple("<apple-id-token>") }
                )

                AuthButton(
                    text = stringResource(R.string.auth_continue_facebook),
                    icon = painterResource(R.drawable.ic_facebook_colored),
                    onClick = { vm.loginFacebook("<facebook-token>") }
                )

                AuthButton(
                    text = stringResource(R.string.auth_continue_email),
                    icon = painterResource(R.drawable.ic_email_colored),
                    onClick = onEmailClick
                )

                AuthButton(
                    text = stringResource(R.string.auth_continue_anonymous),
                    onClick = { vm.loginAnonymous() }
                )
            }
        }
    }
}
