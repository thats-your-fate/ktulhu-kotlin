package com.ktulhu.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ktulhu.ai.R
import com.ktulhu.ai.ui.theme.KColors
import com.ktulhu.ai.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun EmailRegisterScreen(
    vm: AuthViewModel,
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onComplete: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val user by vm.user.collectAsState()
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(user) {
        if (user != null) onComplete()
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            delay(5_000)
            vm.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.auth_create_title), style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.auth_email_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_password_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password)
        )

        uiState.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { vm.registerEmail(email.trim(), password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank() && password.isNotBlank() && !uiState.loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = KColors.btnDefaultBg,
                contentColor = KColors.btnDefaultText,
                disabledContainerColor = KColors.cardBorder,
                disabledContentColor = KColors.cardSubtitle
            )
        ) {
            val buttonText = if (uiState.loading) {
                stringResource(R.string.auth_creating)
            } else {
                stringResource(R.string.auth_create)
            }
            Text(buttonText)
        }

        TextButton(
            onClick = onLoginClick,
            colors = ButtonDefaults.textButtonColors(contentColor = KColors.cardSubtitle)
        ) {
            Text(stringResource(R.string.auth_already_have))
        }

        TextButton(
            onClick = onBack,
            colors = ButtonDefaults.textButtonColors(contentColor = KColors.cardSubtitle)
        ) {
            Text(stringResource(R.string.auth_back))
        }
    }
}
