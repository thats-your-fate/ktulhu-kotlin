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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ktulhu.ai.ui.components.AuthButton
import com.ktulhu.ai.ui.components.TypewriterCarousel
import androidx.compose.ui.res.painterResource
import com.ktulhu.ai.R
import com.ktulhu.ai.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import androidx.compose.ui.platform.LocalContext
import com.ktulhu.ai.ui.theme.KColors
import android.util.Log
import kotlinx.coroutines.delay


@Composable
fun AuthScreen(
    vm: AuthViewModel,
    onEmailClick: () -> Unit,
    onComplete: () -> Unit = {}
) {
    val user by vm.user.collectAsState()
    val uiState by vm.uiState.collectAsState()
    val c = KColors
    val googleClientId = remember(vm.googleClientId) {
        vm.googleClientId.takeIf { it.isNotBlank() }
    }
    val heroMessages = listOf(
        stringResource(R.string.hero_message_workspace),
        stringResource(R.string.hero_message_brainstorm),
        stringResource(R.string.hero_message_secure)
    )
    val context = LocalContext.current

    LaunchedEffect(user) {
        if (user != null) {
            onComplete()
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            delay(5_000)
            vm.clearError()
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

                uiState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Google sign-in launcher
                val googleLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val idToken = account?.idToken
                        if (idToken != null) {
                            vm.loginGoogle(idToken)
                        } else {
                            vm.setError("Google sign-in not configured: missing ID token")
                        }
                    } catch (e: ApiException) {
                        val code = e.statusCode
                        val label = runCatching { GoogleSignInStatusCodes.getStatusCodeString(code) }
                            .getOrNull()
                            ?: "UNKNOWN"
                        vm.setError(
                            "Google sign-in failed ($code: $label). " +
                                "Check OAuth client ID + SHA-1 in Google Cloud Console."
                        )
                    } catch (e: Exception) {
                        vm.setError(e.message ?: "Google sign-in failed")
                    }
                }

                AuthButton(
                    text = stringResource(R.string.auth_continue_google),
                    icon = painterResource(R.drawable.ic_google_colored),
                    tintIcon = false,
                    enabled = true,
                    onClick = {
                        val clientId = googleClientId
                        if (clientId.isNullOrBlank()) {
                            vm.setError("Google client ID not configured")
                            return@AuthButton
                        }
                        Log.d("Auth", "Starting Google sign-in (clientIdSuffix=${clientId.takeLast(10)})")
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(clientId)
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        googleLauncher.launch(client.signInIntent)
                    }
                )

                AuthButton(
                    text = stringResource(R.string.auth_continue_facebook),
                    icon = painterResource(R.drawable.ic_facebook_colored),
                    tintIcon = false,
                    onClick = { vm.loginFacebook("<facebook-token>") }
                )

                AuthButton(
                    text = stringResource(R.string.auth_continue_email),
                    icon = painterResource(R.drawable.ic_email_colored),
                    tintIcon = false,
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
