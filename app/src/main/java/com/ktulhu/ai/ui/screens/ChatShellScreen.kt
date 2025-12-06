package com.ktulhu.ai.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktulhu.ai.data.ServiceLocator
import com.ktulhu.ai.ui.components.SidebarDrawerContent
import com.ktulhu.ai.viewmodel.ChatSummariesViewModel
import com.ktulhu.ai.viewmodel.ChatViewModel
import com.ktulhu.ai.viewmodel.SessionState
import com.ktulhu.ai.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatShellScreen(
    sessionViewModel: SessionViewModel,
    chatViewModel: ChatViewModel = viewModel(),
    summariesViewModel: ChatSummariesViewModel = viewModel()
) {
    val session by sessionViewModel.state.collectAsState()
    val summaries by summariesViewModel.summaries.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ensure WS is connected for this session
    LaunchedEffect(session) {
        if (!ServiceLocator.usingStubData) {
            ServiceLocator.socketManager.ensureConnected(session)
        }
        summariesViewModel.loadInitial(session.deviceHash)
    }

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
                }
            )
        }
    ) {
        MainChatScreen(
            session = session,
            modifier = Modifier,
            chatViewModel = chatViewModel,
            onOpenDrawer = { scope.launch { drawerState.open() } },
            onNewChatShortcut = {
                sessionViewModel.newChat()
                chatViewModel.clear()
            }
        )
    }
}
