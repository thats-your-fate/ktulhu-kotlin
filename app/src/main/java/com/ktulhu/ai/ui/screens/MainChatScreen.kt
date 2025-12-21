package com.ktulhu.ai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ktulhu.ai.ui.components.ChatInputArea
import com.ktulhu.ai.ui.components.ChatMessageBubble
import com.ktulhu.ai.ui.components.ChatTopBar
import com.ktulhu.ai.ui.components.InputStatus
import com.ktulhu.ai.viewmodel.ChatViewModel
import com.ktulhu.ai.viewmodel.SessionState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.roundToInt
import kotlin.math.max

@Composable
fun MainChatScreen(
    session: SessionState,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = viewModel(),
    onOpenDrawer: () -> Unit = {},
    onNewChatShortcut: () -> Unit = {},
    onRenameChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onUploadImage: () -> Unit = {},
    onUploadFile: () -> Unit = {}
) {
    val history by chatViewModel.history.collectAsState()
    val loading by chatViewModel.loading.collectAsState()
    val thinking by chatViewModel.thinking.collectAsState()
    val attachments by chatViewModel.attachments.collectAsState()

    // Layout constants
    val ChatInputMinHeight = 64.dp
    val ChatInputMaxHeight = 200.dp
    val ChatListExtraBottomPadding = 24.dp
    val TopBarHeight = 64.dp

    var composerText by remember(session.chatId) { mutableStateOf("") }
    var chatInputHeightPx by remember { mutableStateOf(0) }
    val chatInputHeightDp = with(LocalDensity.current) { chatInputHeightPx.toDp() }
    val effectiveBottomPadding = if (chatInputHeightDp > ChatInputMinHeight) {
        chatInputHeightDp + ChatListExtraBottomPadding
    } else {
        ChatInputMinHeight + ChatListExtraBottomPadding
    }
    val bottomPaddingPx = with(LocalDensity.current) { effectiveBottomPadding.toPx().roundToInt() }

    val listState = rememberLazyListState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var autoScroll by remember { mutableStateOf(true) }
    var pendingInitialScroll by remember(session.chatId) { mutableStateOf(true) }

    // Load history when chat changes
    LaunchedEffect(session.chatId) {
        chatViewModel.loadHistory(session.chatId)
        autoScroll = true
        pendingInitialScroll = true
    }

    suspend fun scrollToBottom() {
        if (history.isEmpty()) return
        // Use a huge offset so the list clamps to the absolute bottom even if the last item is taller
        // than the viewport (e.g. many streamed tokens).
        listState.scrollToItem(history.lastIndex, Int.MAX_VALUE)
    }

    // Track user intent: update autoScroll only when the user finishes a scroll gesture.
    // If we recompute from layout while content is streaming, autoScroll can flip off mid-stream.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { inProgress -> !inProgress }
            .collect {
                autoScroll = !listState.canScrollForward
            }
    }

    // When a chat loads, jump to the bottom once (no animation) so we're on the latest message.
    LaunchedEffect(session.chatId, history.size) {
        if (!pendingInitialScroll) return@LaunchedEffect
        if (history.isEmpty()) return@LaunchedEffect
        scrollToBottom()
        pendingInitialScroll = false
    }

    // Scroll to last automatically when new message added (animate once).
    LaunchedEffect(history.lastOrNull()?.id) {
        if (history.isNotEmpty() && autoScroll) {
            listState.animateScrollToItem(history.lastIndex, Int.MAX_VALUE)
        }
    }

    LaunchedEffect(history.lastOrNull()?.id to history.lastOrNull()?.role) {
        val last = history.lastOrNull() ?: return@LaunchedEffect
        if (last.role == "assistant") {
            scrollToBottom()
        }
    }

    // Keep bottom visible while streaming into the last message (no animation to prevent jitter).
    LaunchedEffect(session.chatId, listState) {
        snapshotFlow { history.lastOrNull()?.content?.length ?: 0 }
            .distinctUntilChanged()
            .filter { history.isNotEmpty() }
            .collect {
                if (autoScroll) {
                    scrollToBottom()
                }
            }
    }

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
                    top = topInset + TopBarHeight + 12.dp, // status bar + header height + 12dp
                    bottom = effectiveBottomPadding
                )
            ) {
                val lastId = history.lastOrNull()?.id
                items(history, key = { it.id }) { msg ->
                    val showActions = !(thinking && msg.id == lastId && msg.role == "assistant")
                    ChatMessageBubble(
                        msg = msg,
                        showActions = showActions,
                        onRegenerate = { chatViewModel.regenerateAssistantResponse(it, session) },
                        onLike = { chatViewModel.sendMessageFeedback(it, liked = true, session = session) },
                        onDislike = { chatViewModel.sendMessageFeedback(it, liked = false, session = session) }
                    )
                }
            }
        }

        // 2️⃣ Floating top bar (overlay)
        ChatTopBar(
            historyEmpty = history.isEmpty(),
            onOpenDrawer = onOpenDrawer,
            onNewChatShortcut = onNewChatShortcut,
            onRenameChat = onRenameChat,
            onDeleteChat = onDeleteChat,
            onOpenSettings = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        // 3️⃣ Floating input (overlay)
        ChatInputArea(
            value = composerText,
            onValueChange = { composerText = it },
            status = if (thinking) InputStatus.Thinking else InputStatus.Idle,
            enabled = !loading,
            onSend = { text -> chatViewModel.sendPrompt(text, session) },
            attachments = attachments,
            onRemoveAttachment = { chatViewModel.removeAttachment(it) },
            onUploadImage = onUploadImage,
            onUploadFile = onUploadFile,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = ChatInputMinHeight, max = ChatInputMaxHeight)
                .onSizeChanged { size ->
                    chatInputHeightPx = size.height
                }
        )
    }
}
