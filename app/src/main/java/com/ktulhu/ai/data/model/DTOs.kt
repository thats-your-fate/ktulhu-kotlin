package com.ktulhu.ai.data.model

import com.squareup.moshi.Json

data class ChatThreadPayload(
    @Json(name = "chat_id") val chat_id: String? = null,
    @Json(name = "chatId") val chatId: String? = null,
    val messages: List<ChatMessageDto>? = null,
    val thread: List<ChatMessageDto>? = null
)

data class ChatThreadResponse(
    @Json(name = "chat_id") val chat_id: String? = null,
    @Json(name = "chatId") val chatId: String? = null,
    val messages: List<ChatMessageDto>? = null,
    val thread: List<ChatMessageDto>? = null,
    // some backends wrap data under "data"
    val data: ChatThreadPayload? = null
)

data class ChatMessageDto(
    val id: String?,
    val role: String?,
    val text: String?,
    val summary: String?,
    val ts: Long?,
    val language: String? = null,
    // backend may ship message/token instead of text
    val message: String? = null,
    val token: String? = null,
    val attachments: List<ChatMessageAttachmentDto>? = null
)

data class ChatMessageAttachmentDto(
    val id: String?,
    val filename: String?,
    @Json(name = "mime_type") val mime_type: String? = null,
    val path: String?,
    val size: Long? = null,
    val description: String? = null,
    @Json(name = "ocr_text") val ocr_text: String? = null,
    val labels: List<String>? = null
)

data class ChatSummaryResponse(
    @Json(name = "chat_id") val chat_id: String? = null,
    @Json(name = "chatId") val chatId: String? = null,
    val summary: String? = null,
    val text: String? = null,
    val ts: Long? = null,
    // some payloads use "message" instead of "text"
    val message: String? = null
)

data class ChatSummaryListResponse(
    val chats: List<ChatSummaryResponse>? = null,
    @Json(name = "device_hash") val device_hash: String? = null,
    // some backends wrap data under "data"
    val data: ChatSummaryListPayload? = null
)

data class ChatSummaryListPayload(
    val chats: List<ChatSummaryResponse>? = null,
    @Json(name = "device_hash") val device_hash: String? = null
)
