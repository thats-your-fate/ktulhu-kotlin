package com.ktulhu.ai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.ktulhu.ai.data.model.ChatSummary

@Composable
fun SidebarDrawerContent(
    summaries: List<ChatSummary>,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.82f)
    ) {

        Spacer(Modifier.height(16.dp))

        // --- New Chat Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable { onNewChat() }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "New chat",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "New chat",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(8.dp))

        // --- Chat Items ---
        summaries.forEach { chat ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = chat.summary ?: chat.text ?: chat.chatId.take(8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold // ← Make all items semi-bold
                        )
                    )
                },
                selected = false,
                onClick = { onSelectChat(chat.chatId) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(2.dp)) // clean spacing between items
        }

        Spacer(Modifier.height(12.dp))
    }
}
