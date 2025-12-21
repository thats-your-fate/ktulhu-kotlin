package com.ktulhu.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktulhu.ai.R
import com.ktulhu.ai.data.model.ChatAttachment
import com.ktulhu.ai.data.model.ChatMessage
import com.ktulhu.ai.ui.components.markdown.PrimitiveMarkdown
import com.ktulhu.ai.ui.theme.KColors
import android.util.Base64
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
 
@Composable
fun ChatMessageBubble(
    msg: ChatMessage,
    showActions: Boolean = true,
    onRegenerate: ((ChatMessage) -> Unit)? = null,
    onLike: ((ChatMessage) -> Unit)? = null,
    onDislike: ((ChatMessage) -> Unit)? = null
) {
    val c = KColors
    val isUser = msg.role == "user"
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val background = if (isUser) {
        c.messageUserBg
    } else {
        if (isDark) c.messageAssistantBgDark else c.messageAssistantBg
    }
    val textColor = if (isUser) {
        if (isDark) c.messageUserTextDark else c.messageUserText
    } else {
        if (isDark) c.messageAssistantTextDark else c.messageAssistantText
    }
    val actionTint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF6D6D6D)

    val bubbleShape = if (isUser) MaterialTheme.shapes.medium else MaterialTheme.shapes.small
    val fontSize = if (isUser) 16.sp else 18.sp

    // Normalize & parse markdown once
    val parsed: AnnotatedString = remember(msg.content) {
        Log.d("ChatMessageBubble", "Rendering msg ${msg.id} (${msg.role}): ${msg.content}")
        val normalized = decodeUnicodeEscapes(msg.content).replace("\\n", "\n")
        val displayText = if (msg.role == "user") {
            // Hide any extra block appended for the backend (labels/OCR/etc).
            normalized.replace(Regex("(?s)\\n*Attachments description:.*$"), "").trimEnd()
        } else {
            normalized
        }

        runCatching {
            PrimitiveMarkdown.parse(displayText, textColor)
        }.getOrElse {
            AnnotatedString(displayText)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = background,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            shape = bubbleShape,
            modifier = Modifier
        ) {
            Box(
                modifier = Modifier.padding(12.dp)
            ) {
                val content = @Composable {
                    Text(
                        text = parsed,
                        color = textColor,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = fontSize,
                            fontFamily = FontFamily.Default
                        )
                    )
                }

                Column {
                    if (isUser) {
                        UserImagePreviews(msg)
                        if (msg.attachments.any { it.previewBase64 != null } && msg.content.isNotBlank()) {
                            Spacer(Modifier.size(8.dp))
                        }
                        content()
                    } else {
                        SelectionContainer { content() }
                        if (showActions) {
                            Spacer(Modifier.size(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                            ) {
                                if (onRegenerate != null) {
                                    IconButton(
                                        onClick = { onRegenerate(msg) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Refresh,
                                            contentDescription = stringResource(R.string.chat_regenerate),
                                            tint = actionTint
                                        )
                                    }
                                }
                                if (onLike != null) {
                                    IconButton(
                                        onClick = { onLike(msg) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ThumbUp,
                                            contentDescription = stringResource(R.string.chat_like),
                                            tint = actionTint
                                        )
                                    }
                                }
                                if (onDislike != null) {
                                    IconButton(
                                        onClick = { onDislike(msg) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ThumbDown,
                                            contentDescription = stringResource(R.string.chat_dislike),
                                            tint = actionTint
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText("chat", msg.content)
                                        )
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.chat_copied),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = stringResource(R.string.chat_copy),
                                        tint = actionTint
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, msg.content)
                                        }
                                        val chooser = Intent.createChooser(
                                            shareIntent,
                                            context.getString(R.string.chat_share_title)
                                        )
                                        context.startActivity(chooser)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = stringResource(R.string.chat_share),
                                        tint = actionTint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val unicodeRegex = Regex("\\\\u([0-9a-fA-F]{4})")

private fun decodeUnicodeEscapes(raw: String): String {
    if (!raw.contains("\\u")) return raw
    return unicodeRegex.replace(raw) { match ->
        runCatching {
            match.groupValues[1].toInt(16).toChar().toString()
        }.getOrElse { match.value }
    }
}

private val remoteImageClient by lazy { OkHttpClient() }

@Composable
private fun UserImagePreviews(msg: ChatMessage) {
    if (msg.role != "user") return
 
    var openBitmap by remember(msg.id) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val context = LocalContext.current

    val images = msg.attachments
        .filter {
            it.previewBase64 != null || it.mimeType?.startsWith("image", ignoreCase = true) == true
        }
        .take(4)

    if (images.isEmpty()) return
 
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        images.forEach { attachment ->
            val bitmapState = rememberAttachmentBitmapState(attachment, context)
            val imageBitmap = bitmapState.value
            val placeholder: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .width(92.dp)
                        .height(92.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = attachment.filename,
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "attachment",
                    modifier = Modifier
                        .width(92.dp)
                        .height(92.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.06f))
                        .clickable { openBitmap = imageBitmap }
                )
            } else {
                placeholder()
            }
        }
    }
 
    val bmp = openBitmap ?: return
    Dialog(onDismissRequest = { openBitmap = null }) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.92f))
                .padding(12.dp)
        ) {
            Image(
                bitmap = bmp,
                contentDescription = "preview",
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun rememberAttachmentBitmapState(
    attachment: ChatAttachment,
    context: Context
): androidx.compose.runtime.State<androidx.compose.ui.graphics.ImageBitmap?> {
    val previewBase64 = attachment.previewBase64
    val path = attachment.path
    val key = remember(attachment.id, previewBase64, path) { Triple(attachment.id, previewBase64, path) }
    return produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, key1 = key) {
        if (!previewBase64.isNullOrBlank()) {
            value = runCatching {
                val base64 = previewBase64.substringAfter(",", previewBase64)
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
            if (value != null) return@produceState
        }

        if (path.isNullOrBlank()) {
            value = null
            return@produceState
        }

        if (path.startsWith("content://", ignoreCase = true)) {
            value = runCatching {
                context.contentResolver.openInputStream(android.net.Uri.parse(path))?.use { input ->
                    val bytes = input.readBytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }.getOrNull()
            return@produceState
        }

        if (path.startsWith("http", ignoreCase = true)) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val request = Request.Builder().url(path).build()
                    remoteImageClient.newCall(request).execute().use { res ->
                        if (!res.isSuccessful) return@use null
                        val bytes = res.body?.bytes() ?: return@use null
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                }.getOrNull()
            }
            return@produceState
        }

        value = null
    }
}
