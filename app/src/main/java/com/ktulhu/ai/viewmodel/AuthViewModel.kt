package com.ktulhu.ai.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktulhu.ai.data.remote.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class AuthUser(
    val id: String,
    val email: String?,
    val jwt: String?,
    val provider: String
)

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val STORAGE_KEY = "ktulhu_auth_user"
    }

    private val rawGoogleClientId: String = com.ktulhu.ai.BuildConfig.GOOGLE_CLIENT_ID.orEmpty()
    val googleClientId: String = sanitizeClientId(rawGoogleClientId)

    private val prefs: SharedPreferences =
        application.getSharedPreferences("auth_prefs", Application.MODE_PRIVATE)

    private val _user = MutableStateFlow<AuthUser?>(null)
    val user: StateFlow<AuthUser?> = _user

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    var deviceHash: String = ""

    init {
        loadPersistedUser()
    }

    private fun sanitizeClientId(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) return ""
        // local.properties sometimes contains quoted values: KEY="..."
        return trimmed.trim('"').trim()
    }

    private fun loadPersistedUser() {
        val json = prefs.getString(STORAGE_KEY, null) ?: return
        runCatching {
            val obj = JSONObject(json)
            AuthUser(
                id = obj.getString("id"),
                email = obj.optString("email").takeIf { it.isNotBlank() },
                jwt = obj.optString("jwt").takeIf { it.isNotBlank() },
                provider = obj.getString("provider")
            )
        }.onSuccess { _user.value = it }
            .onFailure {
                prefs.edit().remove(STORAGE_KEY).apply()
            }
    }

    private fun persist(user: AuthUser?) {
        if (user == null) {
            prefs.edit().remove(STORAGE_KEY).apply()
        } else {
            val obj = JSONObject().apply {
                put("id", user.id)
                put("provider", user.provider)
                user.email?.let { put("email", it) }
                user.jwt?.let { put("jwt", it) }
            }
            prefs.edit().putString(STORAGE_KEY, obj.toString()).apply()
        }
        _user.value = user
    }

    fun loginAnonymous() {
        val anonUser = AuthUser(
            id = "anon-${UUID.randomUUID()}",
            email = null,
            jwt = null,
            provider = "anonymous"
        )
        persist(anonUser)
        _uiState.value = AuthUiState()
    }

    fun loginGoogle(idToken: String) = launchAuth {
        if (idToken.isBlank() || !idToken.contains(".")) {
            throw IllegalArgumentException("Google sign-in not configured: missing ID token")
        }
        val res = AuthApi.loginGoogle(idToken, deviceHash)
        persist(AuthUser(res.user_id, res.email, res.jwt, "google"))
    }

    fun loginFacebook(accessToken: String) = launchAuth {
        val res = AuthApi.loginFacebook(accessToken, deviceHash)
        persist(AuthUser(res.user_id, res.email, res.jwt, "facebook"))
    }

    fun loginEmail(email: String, password: String) = launchAuth {
        val res = AuthApi.loginEmail(email, password, deviceHash)
        persist(AuthUser(res.user_id, res.email, res.jwt, "email"))
    }

    fun registerEmail(email: String, password: String) = launchAuth {
        validateRegistrationPassword(password)
        val res = AuthApi.registerEmail(email, password, deviceHash)
        persist(AuthUser(res.user_id, res.email, res.jwt, "email"))
    }

    fun logout() {
        persist(null)
        _uiState.value = AuthUiState()
    }

    fun setError(message: String) {
        _uiState.value = AuthUiState(loading = false, error = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun validateRegistrationPassword(password: String) {
        val hasMinLen = password.length >= 8
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasMinLen || !hasUpper || !hasDigit) {
            throw IllegalArgumentException(
                "Password must be at least 8 characters and include 1 uppercase letter and 1 number"
            )
        }
    }

    private fun launchAuth(action: suspend () -> Unit) = viewModelScope.launch {
        _uiState.value = AuthUiState(loading = true)
        try {
            action()
            _uiState.value = AuthUiState()
        } catch (t: Throwable) {
            _uiState.value = AuthUiState(
                loading = false,
                error = t.message ?: "Authentication failed"
            )
        }
    }
}
