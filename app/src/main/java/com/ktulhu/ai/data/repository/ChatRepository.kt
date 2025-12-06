package com.ktulhu.ai.data.repository

import com.ktulhu.ai.data.model.*
import com.ktulhu.ai.data.remote.ChatApi

class ChatRepository(
    private val api: ChatApi,
    private val useStubData: Boolean = false
) {

    suspend fun loadChatThread(chatId: String): List<ChatMessage> {
        if (useStubData) return stubChatThread(chatId)
        val res = api.getChatThread(chatId)
        val messages = res.messages ?: emptyList()

        return messages
            .filter { it.role != "system" && it.role != "summary" }
            .mapIndexed { i, m ->
                ChatMessage(
                    id = m.id ?: "$chatId-$i",
                    role = m.role ?: "assistant",
                    content = m.text ?: m.summary ?: "",
                    ts = m.ts ?: System.currentTimeMillis()
                )
            }
    }

    suspend fun loadChatSummaries(deviceHash: String): List<ChatSummary> {
        if (useStubData) return stubSummaries()
        val res = api.getChatsByDevice(deviceHash)
        val list = res.chats ?: emptyList()
        return list.mapNotNull {
            val id = it.chat_id ?: return@mapNotNull null
            ChatSummary(
                chatId = id,
                summary = cleanSummaryText(it.summary ?: it.text),
                text = cleanSummaryText(it.text),
                ts = it.ts ?: System.currentTimeMillis()
            )
        }.sortedByDescending { it.ts }
    }

    private fun cleanSummaryText(value: String?): String? {
        if (value.isNullOrBlank()) return null
        var t = value.trim()
        t = t.replace(Regex("</s>\\s*$", RegexOption.IGNORE_CASE), "")
        t = t.replace(Regex("^Topic\\s+tag:\\s*", RegexOption.IGNORE_CASE), "")
        t = t.replace(Regex("^\\?\\s*"), "")
        t = t.trim()
        return t.ifBlank { null }
    }

    private fun stubChatThread(chatId: String): List<ChatMessage> {
        val now = System.currentTimeMillis()
        return listOf(
            ChatMessage(
                id = "$chatId-1",
                role = "user",
                content = "Hey Ktulhu, can you summarize Lovecraft's best stories?",
                ts = now - 60_000
            ),
            ChatMessage(
                id = "$chatId-2",
                role = "assistant",
                content = "Sure! The Call of Cthulhu, At the Mountains of Madness, and The Shadow over Innsmouth are perennial favorites.",
                ts = now - 30_000
            )
        )
    }

    private fun stubSummaries(): List<ChatSummary> {
        val now = System.currentTimeMillis()
        return listOf(
            ChatSummary(
                chatId = "demo-1",
                summary = "Brainstorming cosmic horror hooks",
                text = "User asked for plot seeds and the AI suggested cosmic horror twists.",
                ts = now
            ),
            ChatSummary(
                chatId = "demo-2",
                summary = "Anonymous session starter",
                text = "Greetings from Ktulhu Ai.",
                ts = now - 120_000
            )
        )
    }
}
