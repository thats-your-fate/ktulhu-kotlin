package com.ktulhu.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.ktulhu.ai.ui.theme.KColors

@Composable
fun LeftIslandButton(
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit
) {
    val c = KColors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .background(c.messageUserBg)
            .clickable { onOpenDrawer() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = "Menu",
            tint = c.messageUserText,
            modifier = Modifier.size(26.dp)
        )
    }
}
