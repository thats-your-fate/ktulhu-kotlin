package com.ktulhu.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ktulhu.ai.ui.components.KButtonVariant
import com.ktulhu.ai.ui.components.AuthButton
import com.ktulhu.ai.ui.components.KtulhuButton
import com.ktulhu.ai.ui.theme.KColors
import androidx.compose.ui.res.painterResource
import com.ktulhu.ai.R


@Composable
fun AuthScreen(
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onEmailClick: () -> Unit,
    onAnonymousClick: () -> Unit,
    onFacebookClick: () -> Unit = {}
) {
    val c = KColors

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
                    text = "Ktulhu Ai",
                    color = c.headerTitle,
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(Modifier.height(32.dp))

                AuthButton(
                    text = "Continue with Google",
                    icon = painterResource(R.drawable.ic_google_colored),
                    onClick = onGoogleClick
                )

                AuthButton(
                    text = "Continue with Apple",
                    icon = painterResource(R.drawable.ic_apple_filled),
                    onClick = onAppleClick
                )

                AuthButton(
                    text = "Continue with Facebook",
                    icon = painterResource(R.drawable.ic_facebook_colored),
                    onClick = onFacebookClick
                )

                AuthButton(
                    text = "Continue with Email",
                    icon = painterResource(R.drawable.ic_email_colored),
                    onClick = onEmailClick
                )


                AuthButton(
                    text = "Start Anonymous Session",
                    icon = null,
                    onClick = onAnonymousClick
                )

            }
        }
    }
}
