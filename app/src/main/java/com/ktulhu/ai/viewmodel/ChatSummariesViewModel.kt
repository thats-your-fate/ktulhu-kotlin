package com.ktulhu.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktulhu.ai.data.ServiceLocator
import com.ktulhu.ai.data.model.ChatSummary
import com.ktulhu.ai.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatSummariesViewModel : ViewModel() {

    private val repo = ChatRepository(
        api = ServiceLocator.chatApi,
        useStubData = ServiceLocator.usingStubData
    )
    private val socket = ServiceLocator.socketManager

    private val _summaries = MutableStateFlow<List<ChatSummary>>(emptyList())
    val summaries: StateFlow<List<ChatSummary>> = _summaries

    fun loadInitial(deviceHash: String) {
        if (_summaries.value.isNotEmpty()) return // already loaded
        viewModelScope.launch {
            try {
                val list = repo.loadChatSummaries(deviceHash)
                _summaries.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // listen for summary updates via WS
        viewModelScope.launch {
            socket.summaryJsonFlow.collect { json ->
                applyChatSummaryUpdate(json)
            }
        }
    }

    private fun applyChatSummaryUpdate(json: JSONObject) {
        val chatId = json.optString("chat_id", null) ?: return
        val summaryText = clean(
            json.optString("summary", null)
                ?: json.optString("text", null)
                ?: json.optString("message", null)
        )
        val ts = json.optLong("ts", System.currentTimeMillis())

        val updated = ChatSummary(
            chatId = chatId,
            summary = summaryText,
            text = summaryText,
            ts = ts
        )

        _summaries.update { list ->
            val filtered = list.filter { it.chatId != chatId }
            (listOf(updated) + filtered).sortedByDescending { it.ts }
        }
    }

    private fun clean(value: String?): String? {
        if (value.isNullOrBlank()) return null
        var t = value.trim()
        t = t.replace(Regex("</s>\\s*$", RegexOption.IGNORE_CASE), "")
        t = t.replace(Regex("^Topic\\s+tag:\\s*", RegexOption.IGNORE_CASE), "")
        t = t.replace(Regex("^\\?\\s*"), "")
        t = t.trim()
        return t.ifBlank { null }
    }
}
