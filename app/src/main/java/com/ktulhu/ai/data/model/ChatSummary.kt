package com.ktulhu.ai.data.model

data class ChatSummary(
    val chatId: String,
    val summary: String?,
    val text: String?,
    val ts: Long
)
