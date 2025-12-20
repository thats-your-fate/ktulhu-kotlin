package com.ktulhu.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ktulhu.ai.R
import com.ktulhu.ai.ui.theme.KColors

@Composable
fun ChatTopBar(
    historyEmpty: Boolean,
    onOpenDrawer: () -> Unit,
    onNewChatShortcut: () -> Unit,
    onDeleteChat: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
){
    val c = KColors
    val isDark = isSystemInDarkTheme()
    val overlayAlpha = if (isDark) 0.82f else 0.72f
    val backgroundBase = c.appBg
    val backgroundColor = backgroundBase.copy(alpha = overlayAlpha)
    val islandBackground = if (isDark) c.messageUserBg else c.headerBg
    val titleBg = islandBackground
    val titleText = if (isDark) c.messageUserTextDark else c.headerTitle
    val titleBorderColor = if (isDark) c.messageUserBg else c.headerBorder.copy(alpha = 0.6f)

    Surface(
        modifier = modifier,
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LeftIslandButton(
                    onOpenDrawer = onOpenDrawer,
                    modifier = Modifier.size(44.dp)
                )

                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, titleBorderColor, RoundedCornerShape(50))
                        .background(titleBg)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.app_name),
                        color = titleText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.85f,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                RightIsland(
                    isNewChat = historyEmpty,
                    onNewChat = onNewChatShortcut,
                    onRenameChat = {},
                    onDeleteChat = onDeleteChat,
                    onAccountClick = onOpenSettings,
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(44.dp)
                )
            }
        }
    }
}
