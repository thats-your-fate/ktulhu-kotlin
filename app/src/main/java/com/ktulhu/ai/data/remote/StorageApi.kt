package com.ktulhu.ai.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface StorageApi {
    @POST("/api/storage/upload")
    suspend fun upload(@Body body: UploadRequest): UploadResponse
}

data class UploadRequest(
    val filename: String,
    val mime_type: String,
    val data_base64: String
)

data class UploadResponse(
    val filename: String,
    val mime_type: String?,
    val url: String
)
