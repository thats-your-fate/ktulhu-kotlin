package com.ktulhu.ai.data.remote

import com.ktulhu.ai.data.model.ChatSummaryListResponse
import com.ktulhu.ai.data.model.ChatThreadResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ChatApi {
    @GET("/chat-thread/{id}")
    suspend fun getChatThread(@Path("id") id: String): ChatThreadResponse

    @GET("/internal/chats/by-device/{deviceHash}")
    suspend fun getChatsByDevice(@Path("deviceHash") deviceHash: String): ChatSummaryListResponse
}
