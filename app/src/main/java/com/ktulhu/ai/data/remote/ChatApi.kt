package com.ktulhu.ai.data.remote

import com.ktulhu.ai.data.model.ChatSummaryListResponse
import com.ktulhu.ai.data.model.ChatThreadResponse
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface ChatApi {
    @GET("/chat-thread/{id}")
    suspend fun getChatThread(@Path("id") id: String): ChatThreadResponse

    // Raw variant used as a fallback when typed parsing yields empty payloads
    @GET("/chat-thread/{id}")
    suspend fun getChatThreadRaw(@Path("id") id: String): ResponseBody

    @DELETE("/chat-thread/{id}")
    suspend fun deleteChatThread(@Path("id") id: String)

    @GET("/internal/chats/by-device/{deviceHash}")
    suspend fun getChatsByDevice(@Path("deviceHash") deviceHash: String): ChatSummaryListResponse
}
