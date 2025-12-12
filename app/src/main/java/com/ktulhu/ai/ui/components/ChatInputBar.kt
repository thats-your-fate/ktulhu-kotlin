package com.ktulhu.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.ktulhu.ai.ui.theme.KColors
import com.ktulhu.ai.R

enum class InputStatus { Idle, Thinking }

@Composable
fun ChatInputArea(
    value: String,
    onValueChange: (String) -> Unit,
    status: InputStatus,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes placeholderRes: Int = R.string.chat_placeholder,
    enabled: Boolean = true
) {
    val c = KColors
    val shape = RoundedCornerShape(32.dp)
    val placeholder = stringResource(placeholderRes)

    val canSend = enabled && status == InputStatus.Idle

    // 🔥 Dynamic height measurement
    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp * 0.40f // 40vh

    var textHeightPx by remember { mutableStateOf(0) }

    val dynamicHeight = remember(value, textHeightPx) {
        val base = 52.dp
        val measured = (textHeightPx / 1.6f).dp + 32.dp  // convert px → dp, adjust padding
        minOf(maxHeight, maxOf(base, measured))
    }

    fun handleSend() {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || !canSend) return
        onSend(trimmed)
        onValueChange("")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
                contentAlignment = Alignment.BottomEnd
    ) {
        // ------------------------------
        // 🌟 Expanding TextField Container
        // ------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dynamicHeight)
                .border(1.dp, c.cardBorder, shape)
                .background(c.cardBg, shape)
                .padding(start = 16.dp, end = 60.dp, top = 12.dp, bottom = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled && status != InputStatus.Thinking,
                cursorBrush = SolidColor(c.appText),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = c.appText,
                    lineHeight = 22.sp
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { handleSend() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.key == Key.Enter &&
                            !event.isShiftPressed
                        ) {
                            handleSend()
                            true
                        } else false
                    },
                onTextLayout = { layout ->
                    textHeightPx = layout.size.height
                },
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = c.textareaPlaceholder,
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

        // ------------------------------
        // 🚀 Floating Send Button
        // ------------------------------
        StatusButton(
            status = status,
            enabled = canSend && value.isNotBlank(),
            onClick = { handleSend() },
            modifier = Modifier
                .padding(end = 10.dp, bottom = 10.dp)
                .size(44.dp)
        )
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
