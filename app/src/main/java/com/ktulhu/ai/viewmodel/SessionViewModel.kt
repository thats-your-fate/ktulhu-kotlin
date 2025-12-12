package com.ktulhu.ai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ktulhu.ai.util.DeviceFingerprint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class SessionState(
    val deviceHash: String,
    val sessionId: String,
    val chatId: String
)

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val deviceHash: String = DeviceFingerprint.getDeviceHash(application)

    private val _state = MutableStateFlow(
        SessionState(
            deviceHash = deviceHash,
            sessionId = UUID.randomUUID().toString(),
            chatId = UUID.randomUUID().toString()
        )
    )
    val state: StateFlow<SessionState> = _state

    fun newChat() {
        _state.update { it.copy(chatId = UUID.randomUUID().toString()) }
    }

    fun setChatId(id: String) {
        _state.update { it.copy(chatId = id) }
    }
}
