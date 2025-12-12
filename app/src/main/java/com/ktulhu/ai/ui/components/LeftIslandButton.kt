package com.ktulhu.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ktulhu.ai.R
import com.ktulhu.ai.ui.theme.KColors

@Composable
fun LeftIslandButton(
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit
) {
    val c = KColors
    val dark = isSystemInDarkTheme()
    val background = if (dark) c.messageUserBg else Color.White
    val border = if (dark) null else Color(0xFFE5E7EB)
    val content = if (dark) c.messageUserText else Color(0xFF111827)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .then(
                if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(40.dp))
                else Modifier
            )
            .background(background)
            .clickable { onOpenDrawer() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = stringResource(R.string.chat_menu_title),
            tint = content,
            modifier = Modifier.size(28.dp)
        )
    }
}
