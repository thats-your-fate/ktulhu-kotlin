package com.ktulhu.ai.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

object DeviceFingerprint {
    private const val PREFS_NAME = "ktulhu_device_prefs"
    private const val KEY_HASH = "device_hash"

    fun getDeviceHash(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_HASH, null)?.let { return it }

        val info = listOfNotNull(
            Build.BRAND,
            Build.DEVICE,
            Build.MODEL,
            Build.MANUFACTURER,
            Build.PRODUCT,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT.toString(),
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ).joinToString("|")

        val hash = hashString(info).takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        prefs.edit().putString(KEY_HASH, hash).apply()
        return hash
    }

    private fun hashString(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }
}
