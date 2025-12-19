package com.ktulhu.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.ktulhu.ai.R
import com.ktulhu.ai.ui.theme.KColors
@Composable
fun RightIsland(
    isNewChat: Boolean,
    onNewChat: () -> Unit,
    onRenameChat: () -> Unit,
    onDeleteChat: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KColors
    val dark = isSystemInDarkTheme()
    val background = if (dark) c.messageUserBg else Color.White
    val border = if (dark) null else Color(0xFFE5E7EB)
    val content = if (dark) c.messageUserText else Color(0xFF111827)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(50))
                else Modifier
            )
            .background(background)
            .padding(horizontal = 10.dp, vertical = 8.dp) // smaller outer padding
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isNewChat)
                Arrangement.Center
            else
                Arrangement.spacedBy(14.dp) // only apply spacing when 2+ icons
        ) {

            Icon(
                imageVector = Icons.Outlined.Person2,
                contentDescription = stringResource(R.string.chat_login_profile),
                tint = content,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onAccountClick() }
            )

            if (!isNewChat) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_square),
                    contentDescription = stringResource(R.string.chat_new_chat),
                    tint = content,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onNewChat() }
                )

                var showMenu by remember { mutableStateOf(false) }

                Box {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.chat_menu),
                        tint = content,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { showMenu = true }
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_rename)) },
                            onClick = {
                                showMenu = false
                                onRenameChat()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_delete)) },
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
