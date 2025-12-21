package com.ktulhu.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktulhu.ai.data.ServiceLocator
import com.ktulhu.ai.R
import com.ktulhu.ai.ui.components.SidebarDrawerContent
import com.ktulhu.ai.ui.theme.KColors
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
    var renameDialogChatId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

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

    val colors = KColors

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
                .background(colors.appBg)
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
                onRenameChat = {
                    val chatId = session.chatId
                    val summary = summaries.firstOrNull { it.chatId == chatId }
                    val initial = when {
                        summary?.summary?.isNotBlank() == true -> summary.summary!!
                        summary?.text?.isNotBlank() == true -> summary.text!!
                        chatId.isNotBlank() -> chatId
                        else -> return@MainChatScreen
                    }
                    renameText = initial
                    renameDialogChatId = chatId
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

        renameDialogChatId?.let { chatId ->
            val canSave = renameText.isNotBlank()
            AlertDialog(
                onDismissRequest = { renameDialogChatId = null },
                title = { Text(stringResource(R.string.chat_rename)) },
                text = {
                    TextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (canSave) {
                                summariesViewModel.renameChat(chatId, renameText.trim())
                                renameDialogChatId = null
                            }
                        },
                        enabled = canSave
                    ) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { renameDialogChatId = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}
