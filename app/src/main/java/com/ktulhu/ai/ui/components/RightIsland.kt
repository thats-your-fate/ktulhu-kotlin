package com.ktulhu.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ktulhu.ai.ui.theme.KColors
import androidx.compose.material3.Text
import androidx.compose.ui.zIndex

@Composable
fun RightIsland(
    isNewChat: Boolean,
    onNewChat: () -> Unit,
    onRenameChat: () -> Unit,
    onDeleteChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KColors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.messageUserBg)
            .padding(horizontal = 10.dp, vertical = 8.dp) // smaller outer padding
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isNewChat)
                Arrangement.Center
            else
                Arrangement.spacedBy(14.dp) // only apply spacing when 2+ icons
        ) {

            if (isNewChat) {
                Icon(
                    imageVector = Icons.Outlined.Person2,
                    contentDescription = "Login / Profile",
                    tint = c.messageUserText,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onNewChat() }
                )

            } else {

                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "New Chat",
                    tint = c.messageUserText,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onNewChat() }
                )

                var showMenu by remember { mutableStateOf(false) }

                Box {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Chat menu",
                        tint = c.messageUserText,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { showMenu = true }
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename chat") },
                            onClick = {
                                showMenu = false
                                onRenameChat()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete chat") },
                            onClick = {
                                showMenu = false
                                onDeleteChat()
                            }
                        )
                    }
                }
            }
        }
    }
}
