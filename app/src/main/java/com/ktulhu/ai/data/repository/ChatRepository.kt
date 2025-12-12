package com.ktulhu.ai.data.repository

import com.ktulhu.ai.data.model.*
import com.ktulhu.ai.data.remote.ChatApi
import org.json.JSONArray
import org.json.JSONObject

class ChatRepository(
    private val api: ChatApi,
    private val useStubData: Boolean = false
) {

    suspend fun loadChatThread(chatId: String): List<ChatMessage> {
        if (useStubData) return stubChatThread(chatId)
        val raw = api.getChatThreadRaw(chatId).use { it.string() }
        val (resolvedChatId, parsedMessages) = parseThreadJson(raw, chatId)
        var messages: List<ChatMessageDto>? = parsedMessages

        // Fallback to typed decode only if raw parsing failed
        if (messages.isNullOrEmpty()) {
            val res = api.getChatThread(chatId)
            val payload = res.data ?: ChatThreadPayload(
                chat_id = res.chat_id,
                chatId = res.chatId,
                messages = res.messages,
                thread = res.thread
            )
            messages = payload.messages ?: payload.thread
        }

        return messages.orEmpty()
            .filter { msg ->
                val role = msg.role?.lowercase()
                role != "system" && role != "summary"
            }
            .mapIndexed { i, m ->
                val content = m.text
                    ?: m.summary
                    ?: m.message
                    ?: m.token
                    ?: ""
                ChatMessage(
                    id = m.id ?: "$resolvedChatId-$i",
                    role = m.role ?: "assistant",
                    content = cleanContent(content),
                    ts = m.ts ?: System.currentTimeMillis()
                )
            }
    }

    suspend fun loadChatSummaries(deviceHash: String): List<ChatSummary> {
        if (useStubData) return stubSummaries()

        val res = api.getChatsByDevice(deviceHash)
        val payload = res.data ?: ChatSummaryListPayload(
            chats = res.chats,
            device_hash = res.device_hash
        )
        val list = payload.chats ?: emptyList()

        return list.mapNotNull { chat ->
            val chatId = chat.chat_id ?: chat.chatId ?: return@mapNotNull null

            // ---- NEW: load thread and try to find summary message ----
            val rawThread = api.getChatThreadRaw(chatId).use { it.string() }
            var summaryMsg = extractSummaryFromRaw(rawThread)

            val summaryText = summaryMsg?.text
                ?: summaryMsg?.message
                ?: chat.summary
                ?: chat.text
                ?: chat.message

            ChatSummary(
                chatId = chatId,
                summary = cleanSummaryText(summaryText),
                text = cleanSummaryText(chat.text ?: chat.message),
                ts = chat.ts ?: System.currentTimeMillis()
            )
        }.sortedByDescending { it.ts }
    }

    private fun parseThreadJson(raw: String, fallbackChatId: String): Pair<String, List<ChatMessageDto>> {
        return runCatching {
            val obj = JSONObject(raw)
            val resolvedChatId = obj.optString("chat_id")
                .ifBlank { obj.optString("chatId") }
                .ifBlank { fallbackChatId }

            val messagesArray: JSONArray = obj.optJSONArray("messages")
                ?: obj.optJSONArray("thread")
                ?: JSONArray()

            val parsed = mutableListOf<ChatMessageDto>()
            for (i in 0 until messagesArray.length()) {
                val item = messagesArray.optJSONObject(i) ?: continue
                parsed += ChatMessageDto(
                    id = item.optString("id").takeIf { it.isNotBlank() },
                    role = item.optString("role").takeIf { it.isNotBlank() },
                    text = item.optString("text").takeIf { it.isNotBlank() },
                    summary = item.optString("summary").takeIf { it.isNotBlank() },
                    ts = item.optLong("ts", 0L).takeIf { it != 0L },
                        message = item.optString("message").takeIf { it.isNotBlank() },
                        token = item.optString("token").takeIf { it.isNotBlank() }
                )
            }
            resolvedChatId to parsed
        }.getOrDefault(fallbackChatId to emptyList())
    }

    private fun extractSummaryFromRaw(raw: String): ChatMessageDto? {
        return runCatching {
            val obj = JSONObject(raw)
            val messagesArray: JSONArray = obj.optJSONArray("messages")
                ?: obj.optJSONArray("thread")
                ?: JSONArray()
            (0 until messagesArray.length()).asSequence()
                .mapNotNull { idx -> messagesArray.optJSONObject(idx) }
                .firstOrNull { msg ->
                    val role = msg.optString("role").lowercase()
                    val hasContent = msg.optString("text").isNotBlank()
                        || msg.optString("summary").isNotBlank()
                        || msg.optString("message").isNotBlank()
                    role == "summary" && hasContent
                }?.let { m ->
                    ChatMessageDto(
                        id = m.optString("id").takeIf { it.isNotBlank() },
                        role = m.optString("role").takeIf { it.isNotBlank() },
                        text = m.optString("text").takeIf { it.isNotBlank() },
                        summary = m.optString("summary").takeIf { it.isNotBlank() },
                        ts = m.optLong("ts", 0L).takeIf { it != 0L },
                        message = m.optString("message").takeIf { it.isNotBlank() },
                        token = m.optString("token").takeIf { it.isNotBlank() }
                    )
                }
        }.getOrNull()
    }

    private fun cleanContent(value: String): String {
        return value
            .replace("<|im_end|>", "", ignoreCase = true)
            .replace("</s>", "", ignoreCase = true)
            .trim()
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
