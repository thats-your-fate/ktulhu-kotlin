    package com.ktulhu.ai.data.remote

    import com.ktulhu.ai.util.BoundaryFixer
    import com.ktulhu.ai.viewmodel.SessionState
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.channels.BufferOverflow
    import kotlinx.coroutines.flow.MutableSharedFlow
    import kotlinx.coroutines.launch
    import okhttp3.*
    import okio.ByteString
    import org.json.JSONObject
    import java.util.UUID

    class SocketManager(
        private val wsUrl: String,
        private val client: OkHttpClient
    ) {

        sealed class Status { object Idle : Status(); object Connecting : Status()
            object Open : Status(); object Closed : Status(); data class Error(val msg: String) : Status()
        }

        private val scope = CoroutineScope(Dispatchers.IO)
        private val boundaryFixer = BoundaryFixer()
        private var webSocket: WebSocket? = null
        private var inflightId: String? = null
        private var lastSession: SessionState? = null
        private var reconnectBackoffMs = 500L
        private var reconnectJob: Job? = null

        val statusFlow = MutableSharedFlow<Status>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val tokenFlow = MutableSharedFlow<String>(extraBufferCapacity = 128)
        val summaryJsonFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
        val systemJsonFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 16)
        val messageJsonFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
        val doneFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

        data class PromptAttachment(
            val id: String,
            val filename: String,
            val mimeType: String?,
            val localUri: String?,
            val size: Long,
            val description: String? = null, // derived locally (labels/OCR summary) and sent with prompt
            val ocrText: String? = null, // derived locally (OCR) and sent with prompt
            val labels: List<String>? = null,
            val previewBase64: String? = null, // local preview only, not sent
            val remoteUrl: String? = null,
            val uploading: Boolean = false,
            val uploadError: String? = null
        )

        fun ensureConnected(session: SessionState) {
            val previous = lastSession
            lastSession = session
            val ws = webSocket
            if (ws != null) {
                if (previous != null && previous != session) {
                    sendRegister(session)
                }
                return
            }

            connect(session)
        }

        private fun connect(session: SessionState) {
            reconnectJob?.cancel()
            scope.launch { statusFlow.emit(Status.Connecting) }

            val request = Request.Builder().url(wsUrl).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    reconnectJob?.cancel()
                    reconnectBackoffMs = 500
                    scope.launch { statusFlow.emit(Status.Open) }
                    sendRegister(session)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleMessage(text)
                }

                override fun onMessage(ws: WebSocket, bytes: ByteString) {
                    handleMessage(bytes.utf8())
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    webSocket = null
                    scope.launch { statusFlow.emit(Status.Error(t.message ?: "WS error")) }
                    scheduleReconnect()
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    webSocket = null
                    scope.launch { statusFlow.emit(Status.Closed) }
                    scheduleReconnect()
                }
            })
        }

        private fun scheduleReconnect() {
            val session = lastSession ?: return
            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                kotlinx.coroutines.delay(reconnectBackoffMs)
                reconnectBackoffMs = (reconnectBackoffMs * 2).coerceAtMost(8000)
                connect(session)
            }
        }

        private fun sendRegister(session: SessionState) {
            val payload = JSONObject().apply {
                put("msg_type", "register")
                put("request_id", "")
                put("device_hash", session.deviceHash)
                put("session_id", session.sessionId)
                put("chat_id", session.chatId)
                put("text", "")
            }
            webSocket?.send(payload.toString())
        }

        fun sendPrompt(
            text: String,
            session: SessionState,
            attachments: List<PromptAttachment> = emptyList(),
            language: String? = null
        ): String? {
            ensureConnected(session)
            val ws = webSocket ?: return null
            val requestId = UUID.randomUUID().toString()

            val attachmentsArray = org.json.JSONArray().apply {
                attachments.forEach { att ->
                    val obj = JSONObject().apply {
                        put("id", att.id)
                        put("filename", att.filename)
                        att.mimeType?.let { put("mimeType", it) }
                        att.previewBase64?.let { put("previewBase64", it) }
                        val resolvedPath = att.remoteUrl ?: att.localUri
                        resolvedPath?.let { put("path", it) }
                        put("size", att.size)
                        att.description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
                        att.ocrText?.takeIf { it.isNotBlank() }?.let { put("ocrText", it) }
                        att.labels?.takeIf { it.isNotEmpty() }?.let { labels ->
                            put("labels", org.json.JSONArray(labels))
                        }
                    }
                    put(obj)
                }
            }

            val metadata = JSONObject().apply {
                val imageAnalysis = org.json.JSONArray()
                attachments.forEach { att ->
                    val labels = att.labels.orEmpty()
                    val ocr = att.ocrText?.trim().orEmpty()
                    if (labels.isEmpty() && ocr.isBlank()) return@forEach

                    val obj = JSONObject().apply {
                        put("attachment_id", att.id)
                        put("filename", att.filename)
                        att.mimeType?.let { put("mime_type", it) }
                        if (labels.isNotEmpty()) {
                            put("labels", org.json.JSONArray(labels))
                        }
                        if (ocr.isNotBlank()) {
                            put("ocr_text", ocr)
                        }
                        att.description?.takeIf { it.isNotBlank() }?.let { put("summary", it) }
                        put("source", "ml_kit")
                    }
                    imageAnalysis.put(obj)
                }
                if (imageAnalysis.length() > 0) {
                    put("image_analysis", imageAnalysis)
                }
            }.takeIf { it.length() > 0 }

            val payload = JSONObject().apply {
                put("msg_type", "prompt")
                put("request_id", requestId)
                put("chat_id", session.chatId)
                put("session_id", session.sessionId)
                put("device_hash", session.deviceHash)
                put("text", text)
                // Always send `attachments` so serde can deserialize consistently.
                put("attachments", attachmentsArray)
                metadata?.let { put("metadata", it) }
                language?.let { put("language", it) }
            }
            ws.send(payload.toString())
            inflightId = requestId
            return requestId
        }

        fun cancel(session: SessionState) {
            val ws = webSocket ?: return
            val requestId = inflightId ?: UUID.randomUUID().toString()
            val payload = JSONObject().apply {
                put("msg_type", "cancel")
                put("request_id", requestId)
                put("chat_id", session.chatId)
                put("session_id", session.sessionId)
                put("device_hash", session.deviceHash)
                put("text", "")
            }
            ws.send(payload.toString())
            inflightId = null
        }

        private fun handleMessage(raw: String) {
            scope.launch {
                if (!raw.trimStart().startsWith("{")) {
                    if (raw.isNotEmpty()) {
                        val fixed = boundaryFixer.apply(raw)
                        tokenFlow.emit(fixed)
                    }
                    return@launch
                }

                val msg = try {
                    JSONObject(raw)
                } catch (e: Exception) {
                    return@launch
                }

                messageJsonFlow.emit(msg)

                // Some backends send "type", others "msg_type"
                val msgType = msg.optString("type").ifBlank { msg.optString("msg_type") }

                when (msgType) {
                    "system" -> systemJsonFlow.emit(msg)
                    "summary" -> if (msg.has("chat_id")) summaryJsonFlow.emit(msg)
                }

                // token-like payloads (token / text / message)
                val chunk = msg.optString("token").takeIf { it.isNotBlank() }
                    ?: msg.optString("text").takeIf { it.isNotBlank() }
                    ?: msg.optString("message").takeIf { it.isNotBlank() }
                chunk?.let { token ->
                    val safe = token.filter { it.code != 0xFFFD }
                    val fixed = boundaryFixer.apply(safe)
                    tokenFlow.emit(fixed)
                }

                // done
                if (msg.optBoolean("done", false) || msgType == "done") {
                    boundaryFixer.reset()
                    doneFlow.emit(Unit)
                    inflightId = null
                }
            }
        }
    }
