package com.ktulhu.ai.data.remote

import com.ktulhu.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object AttachmentUploader {
    private val client by lazy { OkHttpClient() }
    private const val UPLOAD_ENDPOINT = "https://uploads.ktulhu.com"

    suspend fun upload(bytes: ByteArray, filename: String, mimeType: String?): String =
        withContext(Dispatchers.IO) {
            val mediaType = (mimeType ?: "application/octet-stream").toMediaTypeOrNull()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename, bytes.toRequestBody(mediaType))
                .build()
            val request = Request.Builder()
                .url(UPLOAD_ENDPOINT)
                .post(body)
                .build()
            client.newCall(request).execute().use { res ->
                val payload = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    error("Upload failed (${res.code}): $payload")
                }
                val obj = JSONObject(payload)
                val uuids = obj.optJSONArray("uuids")
                val first = uuids?.optString(0)?.takeIf { it.isNotBlank() }
                    ?: error("Upload response missing uuid")
                val fileBase = BuildConfig.UPLOAD_FILE_BASE_URL.trimEnd('/')
                "$fileBase/$first"
            }
        }
}
