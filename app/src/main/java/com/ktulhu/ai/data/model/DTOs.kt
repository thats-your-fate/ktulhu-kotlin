package com.ktulhu.ai.data.model

data class ChatThreadResponse(
    val chat_id: String,
    val messages: List<ChatMessageDto>?
)

data class ChatMessageDto(
    val id: String?,
    val role: String?,
    val text: String?,
    val summary: String?,
    val ts: Long?
)

data class ChatSummaryResponse(
    val chat_id: String,
    val summary: String?,
    val text: String?,
    val ts: Long?
)

data class ChatSummaryListResponse(
    val chats: List<ChatSummaryResponse>?,
    val device_hash: String?
)
