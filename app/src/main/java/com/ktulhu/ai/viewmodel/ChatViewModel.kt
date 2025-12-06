package com.ktulhu.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktulhu.ai.data.ServiceLocator
import com.ktulhu.ai.data.model.ChatMessage
import com.ktulhu.ai.data.repository.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repo = ChatRepository(
        api = ServiceLocator.chatApi,
        useStubData = ServiceLocator.usingStubData
    )
    private val socket = ServiceLocator.socketManager
    private val usingStubData = ServiceLocator.usingStubData

    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    val history: StateFlow<List<ChatMessage>> = _history

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking

    private var currentAssistantId: String? = null

    init {
        // stream tokens into last assistant message
        if (!usingStubData) {
            viewModelScope.launch {
                socket.tokenFlow.collect { chunk ->
                    val id = currentAssistantId ?: return@collect
                    _history.update { list ->
                        list.map {
                            if (it.id == id) it.copy(content = it.content + chunk) else it
                        }
                    }
                }
            }
            // when done → clear pointer
            viewModelScope.launch {
                socket.doneFlow.collect {
                    currentAssistantId = null
                    _thinking.value = false
                }
            }
        }
    }

    fun loadHistory(chatId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val msgs = repo.loadChatThread(chatId)
                _history.value = msgs
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun sendPrompt(text: String, session: SessionState) {
        if (text.isBlank()) return

        // optimistic user message
        val userMsg = ChatMessage(
            id = "u-${System.currentTimeMillis()}",
            role = "user",
            content = text,
            ts = System.currentTimeMillis()
        )

        // assistant placeholder
        val assistantId = "a-${System.currentTimeMillis()}"
        val assistantMsg = ChatMessage(
            id = assistantId,
            role = "assistant",
            content = "",
            ts = System.currentTimeMillis()
        )

        _history.update { it + userMsg + assistantMsg }
        currentAssistantId = assistantId
        _thinking.value = true

        if (usingStubData) {
            viewModelScope.launch {
                delay(800)
                _history.update { list ->
                    list.map {
                        if (it.id == assistantId) it.copy(
                            content = "This is a local preview response. Configure API_BASE_URL/WS_BASE_URL to talk to the real Ktulhu service."
                        ) else it
                    }
                }
                currentAssistantId = null
                _thinking.value = false
            }
        } else {
            val requestId = socket.sendPrompt(text, session)
            if (requestId == null) {
                _thinking.value = false
                currentAssistantId = null
            }
        }
    }

    fun clear() {
        _history.value = emptyList()
    }
}
