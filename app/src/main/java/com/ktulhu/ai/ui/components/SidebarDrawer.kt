package com.ktulhu.ai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ktulhu.ai.data.model.ChatSummary
import com.ktulhu.ai.R
import com.ktulhu.ai.R.drawable.ic_edit_square
import com.ktulhu.ai.ui.theme.KColors
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun SidebarDrawerContent(
    summaries: List<ChatSummary>,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onRenameChat: (String, String) -> Unit = { _, _ -> },
    activeChatId: String? = null
) {
    val c = KColors
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.82f),
        drawerContainerColor = c.cardBg
    ) {
        var renameTarget by remember { mutableStateOf<ChatSummary?>(null) }
        var renameText by remember { mutableStateOf("") }

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
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = c.cardDivider,
                unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                selectedTextColor = c.cardTitle,
                unselectedTextColor = c.cardTitle,
                selectedIconColor = c.cardTitle,
                unselectedIconColor = c.cardSubtitle
            ),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .heightIn(min = 40.dp) // 👈 default is ~56dp
        )






        // --- Chat Items ---
        summaries.forEach { chat ->
            val isActive = activeChatId == chat.chatId
            var showMenu by remember { mutableStateOf(false) }
            val title = remember(chat.summary, chat.text, chat.chatId) {
                val base = chat.summary ?: chat.text ?: chat.chatId.take(12)
                base.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            Box(
                modifier = Modifier.combinedClickable(
                    onClick = { onSelectChat(chat.chatId) },
                    onLongClick = { showMenu = true }
                )
            ) {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                            )
                        )
                    },
                    selected = isActive,
                    onClick = { onSelectChat(chat.chatId) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = c.cardDivider,
                        unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        selectedTextColor = c.cardTitle,
                        unselectedTextColor = c.cardTitle,
                        selectedIconColor = c.cardTitle,
                        unselectedIconColor = c.cardSubtitle
                    ),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .heightIn(min = 40.dp)
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_open)) },
                        onClick = {
                            showMenu = false
                            onSelectChat(chat.chatId)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_rename)) },
                        onClick = {
                            showMenu = false
                            renameTarget = chat
                            renameText = title
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_delete)) },
                        onClick = {
                            showMenu = false
                            onDeleteChat(chat.chatId)
                        }
                    )
                }
            }

        }

        // Rename dialog
        renameTarget?.let { target ->
            val canSave = renameText.isNotBlank()
            AlertDialog(
                onDismissRequest = { renameTarget = null },
                title = { Text(stringResource(R.string.chat_rename)) },
                text = {
                    TextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (canSave) {
                                onRenameChat(target.chatId, renameText.trim())
                                renameTarget = null
                            }
                        },
                        enabled = canSave
                    ) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { renameTarget = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

    }
}
