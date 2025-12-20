package com.ktulhu.ai.data.model

data class ChatAttachment(
    val id: String,
    val filename: String,
    val mimeType: String?,
    val previewBase64: String?,
    val path: String? = null,
    val description: String? = null,
    val ocrText: String? = null,
    val labels: List<String>? = null
)

data class ChatMessage(
    val id: String,
    val role: String,     // "user" | "assistant" | "system" | "summary"
    val content: String,
    val attachments: List<ChatAttachment> = emptyList(),
    val language: String? = null,
    val ts: Long
)
