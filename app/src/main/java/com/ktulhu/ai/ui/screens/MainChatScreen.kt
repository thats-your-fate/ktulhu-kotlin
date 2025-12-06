package com.ktulhu.ai.ui.screens

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktulhu.ai.ui.components.ChatInputArea
import com.ktulhu.ai.ui.components.ChatMessageBubble
import com.ktulhu.ai.ui.components.InputStatus
import com.ktulhu.ai.ui.theme.KColors
import com.ktulhu.ai.viewmodel.ChatViewModel
import com.ktulhu.ai.viewmodel.SessionState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import com.ktulhu.ai.ui.components.LeftIslandButton
import com.ktulhu.ai.ui.components.RightIsland
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(
    session: SessionState,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = viewModel(),
    onOpenDrawer: () -> Unit = {},
    onNewChatShortcut: () -> Unit = {}
) {
    val c = KColors
    val history by chatViewModel.history.collectAsState()
    val loading by chatViewModel.loading.collectAsState()
    val thinking by chatViewModel.thinking.collectAsState()
    var composerText by remember(session.chatId) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val currentLastIndex by rememberUpdatedState(history.lastIndex)
    val lastMessageKey = history.lastOrNull()?.let { it.id to it.content.length }

    LaunchedEffect(session.chatId) {
        chatViewModel.loadHistory(session.chatId)
    }
    LaunchedEffect(lastMessageKey) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.lastIndex)
        }
    }

    LaunchedEffect(listState, history) {
        if (history.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
            Triple(
                lastVisible?.index,
                lastVisible?.offset?.plus(lastVisible.size) ?: 0,
                layoutInfo.viewportEndOffset
            )
        }.collect { (visibleIndex, bottom, viewportEnd) ->
            if (visibleIndex == currentLastIndex && bottom > viewportEnd) {
                listState.animateScrollBy((bottom - viewportEnd).toFloat())
            }
        }
    }

    val accentColor = if (isSystemInDarkTheme()) c.headerTitle else c.messageUserBg
    val iconContentColor = if (isSystemInDarkTheme()) c.headerTitle else c.messageUserText

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {



                    val isNewChat = history.isEmpty()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // LEFT ISLAND
                        LeftIslandButton(
                            onOpenDrawer = onOpenDrawer,
                            modifier = Modifier.size(44.dp)
                        )

                        // TITLE
                        Surface(
                            color = accentColor,
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                "Ktulhu Ai",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.85f,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // RIGHT ISLAND
                        RightIsland(
                            isNewChat = isNewChat,
                            onNewChat = onNewChatShortcut,
                            onRenameChat = { /* TODO */ },
                            onDeleteChat = { /* TODO */ },
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(44.dp)         // optional: keep consistent height
                        )

                    }






                }
            }
        },

        bottomBar = {
            // Make bottom bar transparent + follow keyboard
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.ime) // <-- KEYBOARD FIX
            ) {
                ChatInputArea(
                    value = composerText,
                    onValueChange = { composerText = it },
                    status = if (thinking) InputStatus.Thinking else InputStatus.Idle,
                    enabled = !loading,
                    onSend = { text ->
                        chatViewModel.sendPrompt(text, session)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp) // LESS padding
                )
            }
        }

    )
{ innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                state = listState
            ) {
                items(history) { msg ->
                    ChatMessageBubble(msg = msg)
                }
            }
        }
    }
}
