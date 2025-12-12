package com.ktulhu.ai.ui.screens

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktulhu.ai.R
import com.ktulhu.ai.ui.components.ChatInputArea
import com.ktulhu.ai.ui.components.ChatMessageBubble
import com.ktulhu.ai.ui.components.InputStatus
import com.ktulhu.ai.ui.components.LeftIslandButton
import com.ktulhu.ai.ui.components.RightIsland
import com.ktulhu.ai.ui.theme.KColors
import com.ktulhu.ai.ui.util.ViewportInfo
import com.ktulhu.ai.viewmodel.ChatViewModel
import com.ktulhu.ai.viewmodel.SessionState
import androidx.compose.foundation.layout.PaddingValues

@Composable
fun MainChatScreen(
    session: SessionState,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = viewModel(),
    onOpenDrawer: () -> Unit = {},
    onNewChatShortcut: () -> Unit = {},
    viewportInfo: ViewportInfo? = null
) {
    val c = KColors

    val history by chatViewModel.history.collectAsState()
    val loading by chatViewModel.loading.collectAsState()
    val thinking by chatViewModel.thinking.collectAsState()

    var composerText by remember(session.chatId) { mutableStateOf("") }

    val listState = rememberLazyListState()
    val currentLastIndex by rememberUpdatedState(history.lastIndex)

    // Load history when chat changes
    LaunchedEffect(session.chatId) {
        chatViewModel.loadHistory(session.chatId)
    }

    // Scroll to last automatically when new message added
    LaunchedEffect(history.lastOrNull()?.id) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.lastIndex)
        }
    }
    // Keep bottom visible while streaming into the last message (content changes but id stays)
    LaunchedEffect(history.lastOrNull()?.let { it.id + ":" + it.content.length }) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.lastIndex)
        }
    }

    // Auto follow-down when user is already at bottom
    LaunchedEffect(listState, history) {
        if (history.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val items = listState.layoutInfo.visibleItemsInfo
            val lastVisible = items.lastOrNull()
            val viewportEnd = listState.layoutInfo.viewportEndOffset
            val bottom = lastVisible?.offset?.plus(lastVisible.size) ?: 0
            Triple(lastVisible?.index, bottom, viewportEnd)
        }.collect { (visibleIndex, bottom, viewportEnd) ->
            if (visibleIndex == currentLastIndex && bottom > viewportEnd) {
                listState.animateScrollBy((bottom - viewportEnd).toFloat())
            }
        }
    }

    val usingViewport = (viewportInfo?.visibleHeight ?: 0) > 0

    // Layout constants
    val ChatInputMinHeight = 64.dp
    val ChatInputMaxHeight = 200.dp
    val ChatListExtraBottomPadding = 24.dp
    val TopBarHeight = 64.dp
    val ExtraBottomPadding = 16.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // 1️⃣ Main content (messages + input)
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(
                    top = TopBarHeight + 8.dp, // 👈 allow scrolling under header
                    bottom = ChatInputMinHeight + ChatListExtraBottomPadding + ExtraBottomPadding
                )
            ) {
                items(history, key = { it.id }) { msg ->
                    ChatMessageBubble(msg)
                }
            }
        }

        // 2️⃣ Floating top bar (overlay)
        ChatTopBar(
            historyEmpty = history.isEmpty(),
            onOpenDrawer = onOpenDrawer,
            onNewChatShortcut = onNewChatShortcut,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(TopBarHeight)
        )

        // 3️⃣ Floating input (overlay)
        ChatInputArea(
            value = composerText,
            onValueChange = { composerText = it },
            status = if (thinking) InputStatus.Thinking else InputStatus.Idle,
            enabled = !loading,
            onSend = { text -> chatViewModel.sendPrompt(text, session) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = ChatInputMinHeight, max = ChatInputMaxHeight)
        )
    }




}

@Composable
private fun ChatTopBar(
    historyEmpty: Boolean,
    onOpenDrawer: () -> Unit,
    onNewChatShortcut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KColors
    val isDark = isSystemInDarkTheme()
    val titleBg = if (isDark) c.messageUserBgDark else Color.White
    val titleText = if (isDark) c.messageUserTextDark else Color(0xFF111827)
    val backgroundColor = c.appBg.copy(alpha = 0.8f) // semi-transparent so chat content peeks through

    Surface(
        modifier = modifier,
        color = backgroundColor,
        tonalElevation = 0.dp
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
                    .then(
                        if (!isDark) Modifier.border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(50))
                        else Modifier
                    )
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
                onDeleteChat = {},
                modifier = Modifier
                    .wrapContentWidth()
                    .height(44.dp)
            )
        }
    }
}
