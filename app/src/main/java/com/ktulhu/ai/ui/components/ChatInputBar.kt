package com.ktulhu.ai.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.ktulhu.ai.ui.theme.KColors
import com.ktulhu.ai.R
import com.ktulhu.ai.data.remote.SocketManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class InputStatus { Idle, Thinking }

@Composable
fun ChatInputArea(
    value: String,
    onValueChange: (String) -> Unit,
    status: InputStatus,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes placeholderRes: Int = R.string.chat_placeholder,
    enabled: Boolean = true,
    attachments: List<SocketManager.PromptAttachment> = emptyList(),
    onRemoveAttachment: (SocketManager.PromptAttachment) -> Unit = {},
    onUploadImage: () -> Unit = {},
    onUploadFile: () -> Unit = {}
) {
    val c = KColors
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(32.dp)
    val placeholder = stringResource(placeholderRes)
    val containerBg = if (isDark) c.textareaBgDark else c.cardBg
    val containerBorder = if (isDark) c.textareaBorderDark else c.cardBorder
    val textColor = if (isDark) c.textareaTextDark else c.textareaText
    val placeholderColor = if (isDark) c.textareaPlaceholderDark else c.textareaPlaceholder
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val canSend = enabled && status == InputStatus.Idle

    // 🔥 Dynamic height measurement
    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp * 0.40f // 40vh

    val attachmentsHeight = if (attachments.isNotEmpty()) 96.dp else 0.dp
    val minHeight = 52.dp + attachmentsHeight

    var hasRequestedInitialFocus by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }

    fun handleSend() {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || !canSend) return
        onSend(trimmed)
        onValueChange("")
    }

    LaunchedEffect(enabled, status) {
        if (enabled && status == InputStatus.Idle) {
            if (!hasRequestedInitialFocus) {
                withFrameNanos { }
                withFrameNanos { }
                focusRequester.requestFocus()
                keyboardController?.show()
                hasRequestedInitialFocus = true
            }
        } else {
            hasRequestedInitialFocus = false
        }
    }

    DisposableEffect(lifecycleOwner, enabled, status) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && enabled && status == InputStatus.Idle) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight, max = maxHeight)
                .border(1.dp, containerBorder, shape)
                .background(containerBg, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (attachments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        attachments.forEach { att ->
                            AttachmentChip(
                                attachment = att,
                                onRemove = { onRemoveAttachment(att) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(containerBg)
                            .border(1.dp, containerBorder, CircleShape)
                            .clickable { showAttachMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.chat_add_attachment),
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                        DropdownMenu(
                            expanded = showAttachMenu,
                            onDismissRequest = { showAttachMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.upload_image)) },
                                onClick = {
                                    showAttachMenu = false
                                    onUploadImage()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.upload_file)) },
                                onClick = {
                                    showAttachMenu = false
                                    onUploadFile()
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            enabled = enabled && status != InputStatus.Thinking,
                            cursorBrush = SolidColor(textColor),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = textColor,
                                lineHeight = 22.sp
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Default
                            ),
                            keyboardActions = KeyboardActions.Default,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (value.isEmpty()) {
                                        Text(
                                            text = placeholder,
                                            color = placeholderColor,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 18.sp,
                                                lineHeight = 22.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    StatusButton(
                        status = status,
                        enabled = canSend && value.isNotBlank(),
                        onClick = { handleSend() },
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }
}


// ------------------------------
// ▶ Status / Send Button
// ------------------------------
@Composable
private fun StatusButton(
    status: InputStatus,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KColors
    val isThinking = status == InputStatus.Thinking

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isThinking) c.badgeBg else c.messageUserBg)
            .clickable(enabled = enabled && !isThinking) { onClick() }
            .padding(start = 3.dp, bottom = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isThinking) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = c.badgeText
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Send,
                contentDescription = stringResource(R.string.chat_send),
                tint = c.messageUserText,
                modifier = Modifier.size(28.dp).rotate(-25f)
            )
        }
    }
}

@Composable
private fun AttachmentChip(
    attachment: SocketManager.PromptAttachment,
    onRemove: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val captionColor = if (isDark) Color.White else Color.Black
    val isImage = attachment.mimeType?.startsWith("image", ignoreCase = true) == true
    val preview: ImageBitmap? = remember(attachment.previewBase64, attachment.mimeType) {
        if (!isImage) return@remember null
        val raw = attachment.previewBase64 ?: return@remember null
        val base64 = raw.substringAfter(",", raw)
        runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = attachment.filename,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = attachment.filename,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clickable { onRemove() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = attachment.filename.take(12),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = captionColor
        )
    }
}
