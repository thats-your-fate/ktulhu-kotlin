package com.ktulhu.ai.data.model

data class ChatMessage(
    val id: String,
    val role: String,     // "user" | "assistant" | "system" | "summary"
    val content: String,
    val ts: Long
)
