package com.ktulhu.ai.data

import com.ktulhu.ai.BuildConfig
import com.ktulhu.ai.data.remote.ChatApi
import com.ktulhu.ai.data.remote.SocketManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ServiceLocator {
    private val okHttp = OkHttpClient.Builder().apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            )
        }
    }.build()

    private val apiBaseUrl = BuildConfig.API_BASE_URL.takeUnless { it.isNullToken() } ?: "https://example.com/"
    private val wsBaseUrl = BuildConfig.WS_BASE_URL.takeUnless { it.isNullToken() } ?: "wss://example.com/ws"
    val usingStubData: Boolean = apiBaseUrl.contains("example.com", ignoreCase = true)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(apiBaseUrl)
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val chatApi: ChatApi = retrofit.create(ChatApi::class.java)

    val socketManager: SocketManager = SocketManager(
        wsUrl = wsBaseUrl,
        client = okHttp
    )

    private fun String.isNullToken(): Boolean = isBlank() || equals("null", ignoreCase = true)
}
