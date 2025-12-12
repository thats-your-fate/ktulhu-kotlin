package com.ktulhu.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktulhu.ai.data.model.ChatMessage
import com.ktulhu.ai.ui.components.markdown.PrimitiveMarkdown
import com.ktulhu.ai.ui.theme.KColors

@Composable
fun ChatMessageBubble(msg: ChatMessage) {
    val c = KColors
    val isUser = msg.role == "user"

    val background = if (isUser) c.messageUserBg else Color.Transparent
    val textColor = if (isUser) c.messageUserText else c.messageAssistantText

    val bubbleShape = if (isUser) MaterialTheme.shapes.medium else MaterialTheme.shapes.small
    val fontSize = if (isUser) 16.sp else 18.sp

    // Normalize & parse markdown once
    val parsed: AnnotatedString = remember(msg.content) {
        val normalized = msg.content.replace("\\n", "\n")

        runCatching {
            PrimitiveMarkdown.parse(normalized, textColor)
        }.getOrElse {
            AnnotatedString(normalized)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = background,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            shape = bubbleShape,
            modifier = if (!isUser) Modifier.padding(vertical = 10.dp) else Modifier
        ) {
            Box(
                modifier = Modifier.padding(12.dp)
            ) {
                val content = @Composable {
                    Text(
                        text = parsed,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize)
                    )
                }

                if (isUser) {
                    content()
                } else {
                    SelectionContainer {
                        content()
                    }
                }
            }
        }
    }
}
