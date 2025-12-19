package com.ktulhu.ai.data.remote

import com.ktulhu.ai.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object AuthApi {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val authAdapter = moshi.adapter(AuthResponse::class.java)
    private val mediaType = "application/json".toMediaType()
    private val BASE = BuildConfig.API_BASE_URL.trimEnd('/') + "/api/auth"

    data class AuthResponse(
        val jwt: String,
        val user_id: String,
        val email: String? = null
    )

    suspend fun loginGoogle(idToken: String, deviceHash: String): AuthResponse =
        post("google", mapOf("id_token" to idToken, "device_hash" to deviceHash), deviceHash)

    suspend fun loginFacebook(token: String, deviceHash: String): AuthResponse =
        post("facebook", mapOf("access_token" to token, "device_hash" to deviceHash), deviceHash)

    suspend fun loginEmail(email: String, password: String, deviceHash: String): AuthResponse =
        post(
            "login",
            mapOf("email" to email, "password" to password, "device_hash" to deviceHash),
            deviceHash
        )

    suspend fun registerEmail(email: String, password: String, deviceHash: String): AuthResponse =
        post(
            "register",
            mapOf("email" to email, "password" to password, "device_hash" to deviceHash),
            deviceHash
        )

    private suspend fun post(
        path: String,
        payload: Map<String, String>,
        deviceHash: String
    ): AuthResponse =
        withContext(Dispatchers.IO) {
            val jsonBody = JSONObject(payload).toString()
            val body = jsonBody.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE/$path")
                .addHeader("X-Device-Hash", deviceHash)
                .post(body)
                .build()

            client.newCall(request).execute().use { res ->
                val text = res.body?.string().orEmpty()
                if (!res.isSuccessful) throw IOException("Auth request to $path failed: $text")
                authAdapter.fromJson(text)
                    ?: throw IOException("Invalid auth response for $path")
            }
        }
}
