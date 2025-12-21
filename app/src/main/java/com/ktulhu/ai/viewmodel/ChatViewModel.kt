package com.ktulhu.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktulhu.ai.data.ServiceLocator
import com.ktulhu.ai.data.model.ChatAttachment
import com.ktulhu.ai.data.model.ChatMessage
import com.ktulhu.ai.data.repository.ChatRepository
import com.ktulhu.ai.data.remote.AttachmentUploader
import com.ktulhu.ai.data.remote.SocketManager.PromptAttachment
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class ChatViewModel : ViewModel() {

    private val repo = ChatRepository(
        api = ServiceLocator.chatApi,
        useStubData = ServiceLocator.usingStubData
    )
    private val socket = ServiceLocator.socketManager
    private val usingStubData = ServiceLocator.usingStubData

    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    val history: StateFlow<List<ChatMessage>> = _history

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking

    private val _attachments = MutableStateFlow<List<PromptAttachment>>(emptyList())
    val attachments: StateFlow<List<PromptAttachment>> = _attachments

    private val languageIdentifier = LanguageIdentification.getClient()

    private var currentAssistantId: String? = null
    private var currentAssistantRequestId: String? = null

    init {
        // stream tokens into last assistant message
        if (!usingStubData) {
            viewModelScope.launch {
                socket.tokenFlow.collect { chunk ->
                    val numeric = chunk.map { it.code }
                    val codes = numeric.joinToString(", ") { String.format("0x%04X", it) }
                    val safeChunk = chunk.replace("\n", "\\n")
                    Log.d("ChatTokenChunk", "chunk='$safeChunk' codes=[$codes] rawCodes=$numeric")
                    val id = currentAssistantId ?: return@collect
                    _history.update { list ->
                        list.map {
                            if (it.id == id) it.copy(content = it.content + chunk) else it
                        }
                    }
                }
            }
            // when done → clear pointer
            viewModelScope.launch {
                socket.doneFlow.collect {
                    currentAssistantId = null
                    currentAssistantRequestId = null
                    _thinking.value = false
                }
            }

            viewModelScope.launch {
                socket.systemJsonFlow.collect { json ->
                    handleSystemMessage(json)
                }
            }

            viewModelScope.launch {
                socket.messageJsonFlow.collect { json ->
                    handleSocketMessageMetadata(json)
                }
            }
        }
    }

    fun loadHistory(chatId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val msgs = repo.loadChatThread(chatId)
                _history.value = msgs
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun sendPrompt(text: String, session: SessionState) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val attachmentsSnapshot = attachments.value
            val detectedLanguage = detectLanguage(text)
            val messageAttachments = attachmentsSnapshot.map { att ->
                val resolvedPath = att.remoteUrl ?: att.localUri
                ChatAttachment(
                    id = att.id,
                    filename = att.filename,
                    mimeType = att.mimeType,
                    previewBase64 = att.previewBase64,
                    path = resolvedPath,
                    description = att.description,
                    ocrText = att.ocrText,
                    labels = att.labels
                )
            }

            // optimistic user message
            val userMsg = ChatMessage(
                id = "u-${System.currentTimeMillis()}",
                role = "user",
                content = text,
                attachments = messageAttachments,
                language = detectedLanguage,
                ts = System.currentTimeMillis()
            )

            // assistant placeholder
            val assistantId = "a-${System.currentTimeMillis()}"
            val assistantMsg = ChatMessage(
                id = assistantId,
                role = "assistant",
                content = "",
                ts = System.currentTimeMillis()
            )

            _history.update { it + userMsg + assistantMsg }
            currentAssistantId = assistantId
            _thinking.value = true

            if (usingStubData) {
                delay(800)
                _history.update { list ->
                    list.map {
                        if (it.id == assistantId) it.copy(
                            content = "This is a local preview response. Configure API_BASE_URL/WS_BASE_URL to talk to the real Ktulhu service."
                        ) else it
                    }
                }
                currentAssistantId = null
                _thinking.value = false
            } else {
                val requestId = socket.sendPrompt(
                    text = text,
                    session = session,
                    attachments = attachmentsSnapshot,
                    language = detectedLanguage
                )
                if (requestId == null) {
                    _thinking.value = false
                    currentAssistantId = null
                    currentAssistantRequestId = null
                } else {
                    currentAssistantRequestId = requestId
                }
            }

            // clear attachments after sending
            _attachments.value = emptyList()
        }
    }

    fun clear() {
        _history.value = emptyList()
    }

    fun clearAttachments() {
        _attachments.value = emptyList()
    }

    fun addAttachment(att: PromptAttachment) {
        _attachments.update { it + att }
    }

    fun removeAttachment(att: PromptAttachment) {
        _attachments.update { list -> list.filterNot { it == att } }
    }
    fun regenerateAssistantResponse(target: ChatMessage, session: SessionState) {
        viewModelScope.launch {
            val snapshot = history.value
            if (snapshot.isEmpty()) return@launch
            val targetIndex = snapshot.indexOfLast { it.id == target.id }
            if (targetIndex == -1) return@launch
            val targetMessage = snapshot[targetIndex]
            if (targetMessage.role != "assistant" || targetIndex != snapshot.lastIndex) return@launch
            val previousUser = snapshot.take(targetIndex).lastOrNull { it.role == "user" } ?: return@launch
            val remoteId = targetMessage.serverId ?: targetMessage.id

            val deleteSucceeded = if (usingStubData) {
                true
            } else {
                repo.deleteMessage(session.chatId, remoteId)
            }
            if (!deleteSucceeded) return@launch

            val newAssistantId = "a-${System.currentTimeMillis()}"
            val placeholder = ChatMessage(
                id = newAssistantId,
                role = "assistant",
                content = "",
                ts = System.currentTimeMillis()
            )
            _history.update { list ->
                list.filterNot { it.id == targetMessage.id } + placeholder
            }
            currentAssistantId = newAssistantId
            _thinking.value = true

            if (usingStubData) {
                delay(800)
                _history.update { list ->
                    list.map {
                        if (it.id == newAssistantId) it.copy(content = "This is a regenerated stub response.") else it
                    }
                }
                currentAssistantId = null
                _thinking.value = false
            } else {
                val requestId = socket.sendPrompt(
                    text = previousUser.content,
                    session = session
                )
                if (requestId == null) {
                    _history.update { list -> list.filterNot { it.id == newAssistantId } }
                    _thinking.value = false
                    currentAssistantId = null
                    currentAssistantRequestId = null
                } else {
                    currentAssistantRequestId = requestId
                }
            }
        }
    }

    fun sendMessageFeedback(message: ChatMessage, liked: Boolean, session: SessionState) {
        if (usingStubData) return
        val remoteId = message.serverId ?: message.id
        if (remoteId.isBlank()) return
        viewModelScope.launch {
            repo.setMessageLiked(session.chatId, remoteId, liked)
        }
    }

    private fun handleSocketMessageMetadata(json: JSONObject) {
        val messageId = json.optString("message_id")
            .ifBlank { json.optString("messageId") }
            .ifBlank { json.optString("id") }
        if (messageId.isBlank()) return

        val role = json.optString("role")
            .ifBlank { json.optString("msg_role") }
        val requestId = json.optString("request_id")
            .ifBlank { json.optString("requestId") }

        val matchesAssistantRole = role.equals("assistant", ignoreCase = true)
        val matchesRequest = requestId.isNotBlank() && requestId == currentAssistantRequestId
        if (!matchesAssistantRole && !matchesRequest) return

        val targetId = currentAssistantId ?: return
        _history.update { list ->
            list.map { current ->
                if (current.id == targetId && current.serverId.isNullOrBlank()) {
                    current.copy(serverId = messageId)
                } else {
                    current
                }
            }
        }
    }

    private fun updateAttachment(id: String, transform: (PromptAttachment) -> PromptAttachment) {
        _attachments.update { list ->
            list.map { current ->
                if (current.id == id) transform(current) else current
            }
        }
    }

    private fun startAttachmentUpload(att: PromptAttachment, bytes: ByteArray) {
        viewModelScope.launch {
            updateAttachment(att.id) { it.copy(uploading = true, uploadError = null) }
            val uploadResult = runCatching {
                AttachmentUploader.upload(bytes, att.filename, att.mimeType)
            }
            uploadResult.onSuccess { url ->
                updateAttachment(att.id) { it.copy(remoteUrl = url, uploading = false, uploadError = null) }
                propagateAttachmentPath(att.id, url)
            }.onFailure { e ->
                updateAttachment(att.id) { it.copy(uploading = false, uploadError = e.message) }
            }
        }
    }

    private fun propagateAttachmentPath(attId: String, remoteUrl: String) {
        _history.update { list ->
            list.map { msg ->
                val updatedAttachments = msg.attachments.map { chatAtt ->
                    if (chatAtt.id == attId && chatAtt.path != remoteUrl) {
                        chatAtt.copy(path = remoteUrl)
                    } else {
                        chatAtt
                    }
                }
                if (updatedAttachments == msg.attachments) msg else msg.copy(attachments = updatedAttachments)
            }
        }
    }

    fun attachFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val (name, mime) = resolveMeta(context, uri)
                val bytes = readBytes(context, uri)
                val isImage = mime.startsWith("image/", ignoreCase = true)
                val isPdf = mime.equals("application/pdf", ignoreCase = true)
                val isPlainText = isPlainTextMime(mime)

                val (previewDataUri, normalizedLabels, normalizedOcr) = when {
                    isImage -> {
                        val preview = "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val (labels, ocrText) = analyzeImage(bytes)
                        Triple(
                            preview,
                            labels.filter { it.isNotBlank() },
                            ocrText?.takeIf { it.isNotBlank() }
                        )
                    }
                    isPdf -> {
                        val pdfInfo = analyzePdf(context, uri)
                        Triple(
                            pdfInfo.previewBase64,
                            pdfInfo.labels,
                            pdfInfo.ocrText
                        )
                    }
                    isPlainText -> {
                        val textContent = extractPlainText(context, uri)
                        Triple(
                            null,
                            emptyList<String>(),
                            textContent?.takeIf { it.isNotBlank() }
                        )
                    }
                    else -> Triple(null, emptyList(), null)
                }

                val attachmentDescription = buildAttachmentDescription(normalizedLabels, normalizedOcr)
                val payload = PromptAttachment(
                    id = UUID.randomUUID().toString(),
                    filename = name,
                    mimeType = mime,
                    localUri = uri.toString(), // local-only reference
                    size = bytes.size.toLong(),
                    description = attachmentDescription,
                    ocrText = normalizedOcr,
                    labels = normalizedLabels.takeIf { it.isNotEmpty() },
                    previewBase64 = previewDataUri,
                    uploading = true
                )
                addAttachment(payload)
                startAttachmentUpload(payload, bytes)
            }.onFailure { it.printStackTrace() }
        }
    }

    private suspend fun analyzeImage(bytes: ByteArray): Pair<List<String>, String?> = runCatching {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching emptyList<String>() to null
        val image = InputImage.fromBitmap(bitmap, 0)
        val labels = deriveImageLabels(image)
        val ocrText = deriveImageOcrText(image)
        labels to ocrText
    }.getOrDefault(emptyList<String>() to null)

    private data class PdfAnalysis(
        val previewBase64: String?,
        val labels: List<String>,
        val ocrText: String?
    )

    private suspend fun analyzePdf(context: Context, uri: Uri): PdfAnalysis = withContext(Dispatchers.IO) {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext PdfAnalysis(null, emptyList(), null)
        val renderer = PdfRenderer(descriptor)
        val labels = mutableListOf<String>()
        val ocrPieces = mutableListOf<String>()
        var preview: String? = null
        val pageLimit = minOf(renderer.pageCount, 3)
        try {
            for (pageIndex in 0 until pageLimit) {
                val page = renderer.openPage(pageIndex)
                val baseWidth = page.width.coerceAtLeast(1)
                val baseHeight = page.height.coerceAtLeast(1)
                val scale = 2f
                val bitmap = Bitmap.createBitmap(
                    (baseWidth * scale).roundToInt().coerceAtLeast(1),
                    (baseHeight * scale).roundToInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                val matrix = Matrix().apply { setScale(scale, scale) }
                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                if (preview == null) {
                    preview = bitmapToDataUri(bitmap)
                }
                val image = InputImage.fromBitmap(bitmap, 0)
                labels += deriveImageLabels(image)
                deriveImageOcrText(image)?.let { ocrPieces += it }
                bitmap.recycle()
            }
        } finally {
            renderer.close()
            descriptor.close()
        }
        val normalizedLabels = labels.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val mergedOcr = ocrPieces.joinToString("\n").trim().takeIf { it.isNotBlank() }
        PdfAnalysis(preview, normalizedLabels, mergedOcr)
    }

    private suspend fun deriveImageLabels(image: InputImage): List<String> {
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val labels = suspendCancellableCoroutine<List<com.google.mlkit.vision.label.ImageLabel>> { cont ->
            cont.invokeOnCancellation { runCatching { labeler.close() } }
            labeler.process(image)
                .addOnSuccessListener { res ->
                    runCatching { labeler.close() }
                    cont.resume(res)
                }
                .addOnFailureListener { e ->
                    runCatching { labeler.close() }
                    cont.resumeWithException(e)
                }
        }
        val top = labels
            .sortedByDescending { it.confidence }
            .take(3)
            .map { it.text }
            .filter { it.isNotBlank() }
        return top
    }

    private suspend fun deriveImageOcrText(image: InputImage): String? {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val text = suspendCancellableCoroutine<String> { cont ->
            cont.invokeOnCancellation { runCatching { recognizer.close() } }
            recognizer.process(image)
                .addOnSuccessListener { res ->
                    runCatching { recognizer.close() }
                    cont.resume(res.text ?: "")
                }
                .addOnFailureListener { e ->
                    runCatching { recognizer.close() }
                    cont.resumeWithException(e)
                }
        }
        return text.trim().takeIf { it.isNotBlank() }
    }

    private fun buildAttachmentDescription(labels: List<String>, ocrText: String?): String? {
        val parts = mutableListOf<String>()
        if (labels.isNotEmpty()) {
            parts += labels.joinToString(", ")
        }
        val normalizedOcr = ocrText
            ?.split('\n')
            ?.joinToString(" ") { it.trim() }
            ?.replace("\\s+".toRegex(), " ")
            ?.trim()
        if (!normalizedOcr.isNullOrBlank()) {
            val snippet = normalizedOcr.take(400)
            parts += "OCR: $snippet"
        }
        return parts.joinToString(" | ").takeIf { it.isNotBlank() }
    }

    private fun bitmapToDataUri(bitmap: Bitmap): String? {
        val stream = ByteArrayOutputStream()
        val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return if (ok) {
            val bytes = stream.toByteArray()
            "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } else {
            null
        }
    }

    private fun extractPlainText(context: Context, uri: Uri, maxChars: Int = 8000): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    val buffer = CharArray(2048)
                    val sb = StringBuilder()
                    var total = 0
                    while (true) {
                        val read = reader.read(buffer)
                        if (read <= 0 || total >= maxChars) break
                        val toAppend = buffer.concatToString(0, read)
                        val remaining = maxChars - total
                        val chunk = if (toAppend.length > remaining) {
                            toAppend.substring(0, remaining)
                        } else {
                            toAppend
                        }
                        sb.append(chunk)
                        total += chunk.length
                        if (total >= maxChars) break
                    }
                    sb.toString()
                }
            }
        }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun isPlainTextMime(mime: String): Boolean {
        val lower = mime.lowercase()
        return lower.startsWith("text/") ||
            "json" in lower ||
            "xml" in lower ||
            "yaml" in lower ||
            "csv" in lower ||
            "javascript" in lower
    }

    private fun resolveMeta(context: Context, uri: Uri): Pair<String, String> {
        var name = "attachment"
        var mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIdx >= 0) {
                cursor.getString(nameIdx)?.let { name = it }
            }
        }
        return name to mime
    }

    private fun readBytes(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val stream: InputStream = resolver.openInputStream(uri) ?: error("Cannot open stream")
        return stream.use { it.readBytes() }
    }

    private fun handleSystemMessage(json: JSONObject) {
        val text = json.optString("text").ifBlank {
            json.optString("message").ifBlank { json.optString("token") }
        }
        if (text.isNotBlank()) {
            val systemMsg = ChatMessage(
                id = "sys-${System.currentTimeMillis()}",
                role = "system",
                content = text,
                language = null,
                ts = System.currentTimeMillis()
            )
            _history.update { it + systemMsg }
        }

        if (json.optBoolean("done", false)) {
            currentAssistantId = null
            _thinking.value = false
        }
    }

    private suspend fun detectLanguage(text: String): String? {
        if (text.isBlank()) return null

        val mlKitLang = runCatching {
            suspendCancellableCoroutine<String> { cont ->
                languageIdentifier.identifyLanguage(text)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume("und") }
            }
        }.getOrNull()

        if (!mlKitLang.isNullOrBlank() && mlKitLang != "und") {
            return mlKitLang
        }

        return if (containsCyrillic(text)) detectCyrillicLanguage(text) else null
    }

    private fun containsCyrillic(text: String): Boolean {
        for (ch in text) {
            val block = Character.UnicodeBlock.of(ch)
            if (
                block == Character.UnicodeBlock.CYRILLIC ||
                block == Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY ||
                block == Character.UnicodeBlock.CYRILLIC_EXTENDED_A ||
                block == Character.UnicodeBlock.CYRILLIC_EXTENDED_B
            ) {
                return true
            }
        }
        return false
    }

    private fun detectCyrillicLanguage(text: String): String {
        return when {
            text.any { it in "іїєґ" } -> "uk"
            text.any { it in "ў" } -> "be"
            else -> "ru"
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { languageIdentifier.close() }
    }
}
