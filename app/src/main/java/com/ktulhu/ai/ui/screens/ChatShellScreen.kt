package com.ktulhu.ai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktulhu.ai.data.ServiceLocator
import com.ktulhu.ai.ui.components.SidebarDrawerContent
import com.ktulhu.ai.ui.util.rememberViewportInfo
import com.ktulhu.ai.viewmodel.ChatSummariesViewModel
import com.ktulhu.ai.viewmodel.ChatViewModel
import com.ktulhu.ai.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatShellScreen(
    sessionViewModel: SessionViewModel,
    chatViewModel: ChatViewModel = viewModel(),
    summariesViewModel: ChatSummariesViewModel = viewModel(),
    onOpenSettings: () -> Unit = {}
) {
    val session by sessionViewModel.state.collectAsState()
    val summaries by summariesViewModel.summaries.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    chatViewModel.attachFromUri(context, it)
                }
            }
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    chatViewModel.attachFromUri(context, it)
                }
            }
        }
    }

    // Load summaries + establish WS
    LaunchedEffect(session) {
        if (!ServiceLocator.usingStubData) {
            ServiceLocator.socketManager.ensureConnected(session)
        }
        summariesViewModel.loadInitial(session.deviceHash)
    }

    val viewport = rememberViewportInfo()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawerContent(
                summaries = summaries,
                onNewChat = {
                    sessionViewModel.newChat()
                    scope.launch { drawerState.close() }
                },
                onSelectChat = { id ->
                    sessionViewModel.setChatId(id)
                    scope.launch { drawerState.close() }
                },
                onDeleteChat = { id ->
                    summariesViewModel.deleteChat(id)
                    if (session.chatId == id) {
                        sessionViewModel.newChat()
                        chatViewModel.clear()
                    }
                },
                onRenameChat = { id, title ->
                    summariesViewModel.renameChat(id, title)
                },
                activeChatId = session.chatId
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 0.dp,
                        bottom = 12.dp
                    )
                )
        ) {
            MainChatScreen(
                session = session,
                chatViewModel = chatViewModel,
                modifier = Modifier
                    .fillMaxSize(),
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onNewChatShortcut = {
                    sessionViewModel.newChat()
                    chatViewModel.clear()
                },
                onDeleteChat = {
                    val currentId = session.chatId
                    if (currentId.isNotBlank()) {
                        summariesViewModel.deleteChat(currentId)
                    }
                    sessionViewModel.newChat()
                    chatViewModel.clear()
                },
                onOpenSettings = onOpenSettings,
                onUploadImage = { pickImageLauncher.launch("image/*") },
                onUploadFile = { pickFileLauncher.launch("*/*") }
            )
        }
    }
}
