package com.ktulhu.ai.ui.components


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ktulhu.ai.data.model.ChatSummary
import com.ktulhu.ai.R
import com.ktulhu.ai.R.drawable.ic_edit_square
@Composable
fun SidebarDrawerContent(
    summaries: List<ChatSummary>,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.82f)
    ) {

        Spacer(Modifier.height(8.dp))

        // --- New Chat Row ---
        NavigationDrawerItem(
            label = {
                Text(
                    text = stringResource(R.string.chat_new_chat),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_square),
                    contentDescription = stringResource(R.string.chat_new_chat)
                )
            },
            selected = false,
            onClick = onNewChat,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .heightIn(min = 40.dp) // 👈 default is ~56dp
        )






        // --- Chat Items ---
        summaries.forEach { chat ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = chat.summary ?: chat.text ?: chat.chatId.take(12),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                selected = false,
                onClick = { onSelectChat(chat.chatId) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .heightIn(min = 40.dp)
            )



        }

    }
}
