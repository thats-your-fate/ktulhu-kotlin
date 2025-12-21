package com.ktulhu.ai.data.repository

import com.ktulhu.ai.data.model.*
import com.ktulhu.ai.data.remote.ChatApi
import com.ktulhu.ai.data.remote.MessageLikedRequest
import com.ktulhu.ai.data.remote.SummaryUpdateRequest
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
                val resolvedId = m.id ?: "$resolvedChatId-$i"
                ChatMessage(
                    id = resolvedId,
                    serverId = m.id,
                    role = m.role ?: "assistant",
                    content = cleanContent(content),
                    attachments = m.attachments.orEmpty().mapNotNull { att ->
                        val path = att.path?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        ChatAttachment(
                            id = att.id ?: path,
                            filename = att.filename ?: "attachment",
                            mimeType = att.mime_type,
                            previewBase64 = att.previewBase64,
                            path = path,
                            description = att.description,
                            ocrText = att.ocr_text,
                            labels = att.labels
                        )
                    },
                    language = m.language,
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
            val (parsedId, parsedMessages) = parseThreadJson(rawThread, chatId)
            var summaryMsg = extractSummaryFromRaw(rawThread)

            val summaryText = summaryMsg?.text
                ?: summaryMsg?.message
                ?: chat.summary
                ?: chat.text
                ?: chat.message

            // compute recency using latest message or summary ts
            val latestMsgTsFromParsed = parsedMessages.maxOfOrNull { it.ts ?: 0L } ?: 0L
            val summaryTs = summaryMsg?.ts ?: 0L
            val chatTs = chat.ts ?: 0L
            val resolvedTs = listOf(latestMsgTsFromParsed, summaryTs, chatTs)
                .maxOrNull()
                ?.takeIf { it > 0 } ?: System.currentTimeMillis()

            ChatSummary(
                chatId = parsedId.takeUnless { it.isBlank() } ?: chatId,
                summary = cleanSummaryText(summaryText),
                text = cleanSummaryText(chat.text ?: chat.message),
                ts = resolvedTs
            )
        }.sortedByDescending { it.ts }
    }

    suspend fun deleteChat(chatId: String) {
        if (useStubData) return
        runCatching { api.deleteChatThread(chatId) }.onFailure { it.printStackTrace() }
    }

    suspend fun updateChatSummary(chatId: String, summary: String) {
        if (useStubData) return
        runCatching {
            api.updateChatSummary(chatId, SummaryUpdateRequest(summary = summary))
        }.onFailure { it.printStackTrace() }
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Boolean {
        if (useStubData) return true
        return runCatching {
            api.deleteMessage(chatId, messageId)
            true
        }.onFailure { it.printStackTrace() }
            .getOrElse { false }
    }

    suspend fun setMessageLiked(chatId: String, messageId: String, liked: Boolean) {
        if (useStubData) return
        runCatching { api.setMessageLiked(chatId, messageId, MessageLikedRequest(liked = liked)) }
            .onFailure { it.printStackTrace() }
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
                val attsArray = item.optJSONArray("attachments") ?: JSONArray()
                val attachments = buildList {
                    for (j in 0 until attsArray.length()) {
                        val objAtt = attsArray.optJSONObject(j) ?: continue
                        val labelsArray = objAtt.optJSONArray("labels")
                        val labels = if (labelsArray != null && labelsArray.length() > 0) {
                            buildList {
                                for (k in 0 until labelsArray.length()) {
                                    labelsArray.optString(k)?.takeIf { it.isNotBlank() }?.let { add(it) }
                                }
                            }.takeIf { it.isNotEmpty() }
                        } else null
                        add(
                            ChatMessageAttachmentDto(
                                id = objAtt.optString("id").takeIf { it.isNotBlank() },
                                filename = objAtt.optString("filename").takeIf { it.isNotBlank() },
                                mime_type = objAtt.optString("mime_type").takeIf { it.isNotBlank() },
                                path = objAtt.optString("path").takeIf { it.isNotBlank() },
                                size = objAtt.optLong("size", 0L).takeIf { it > 0L },
                                description = objAtt.optString("description").takeIf { it.isNotBlank() },
                                ocr_text = objAtt.optString("ocr_text").takeIf { it.isNotBlank() },
                                labels = labels,
                                previewBase64 = objAtt.optString("preview_base64")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: objAtt.optString("previewBase64").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
                parsed += ChatMessageDto(
                    id = item.optString("id").takeIf { it.isNotBlank() },
                    role = item.optString("role").takeIf { it.isNotBlank() },
                    text = item.optString("text").takeIf { it.isNotBlank() },
                    summary = item.optString("summary").takeIf { it.isNotBlank() },
                    language = item.optString("language").takeIf { it.isNotBlank() },
                    ts = item.optLong("ts", 0L).takeIf { it != 0L },
                    message = item.optString("message").takeIf { it.isNotBlank() },
                    token = item.optString("token").takeIf { it.isNotBlank() },
                    attachments = attachments.takeIf { it.isNotEmpty() }
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
                        language = m.optString("language").takeIf { it.isNotBlank() },
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
                serverId = "$chatId-1",
                role = "user",
                content = "Hey Ktulhu, can you summarize Lovecraft's best stories?",
                language = "en",
                ts = now - 60_000
            ),
            ChatMessage(
                id = "$chatId-2",
                serverId = "$chatId-2",
                role = "assistant",
                content = "Sure! The Call of Cthulhu, At the Mountains of Madness, and The Shadow over Innsmouth are perennial favorites.",
                language = "en",
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
