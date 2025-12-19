package com.ktulhu.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktulhu.ai.data.ServiceLocator
import com.ktulhu.ai.data.model.ChatAttachment
import com.ktulhu.ai.data.model.ChatMessage
import com.ktulhu.ai.data.repository.ChatRepository
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
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.InputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

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

    init {
        // stream tokens into last assistant message
        if (!usingStubData) {
            viewModelScope.launch {
                socket.tokenFlow.collect { chunk ->
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
                    _thinking.value = false
                }
            }

            viewModelScope.launch {
                socket.systemJsonFlow.collect { json ->
                    handleSystemMessage(json)
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

            // optimistic user message
            val userMsg = ChatMessage(
                id = "u-${System.currentTimeMillis()}",
                role = "user",
                content = text,
                attachments = attachmentsSnapshot.map { att ->
                    ChatAttachment(
                        id = att.id,
                        filename = att.filename,
                        mimeType = att.mimeType,
                        previewBase64 = att.previewBase64,
                        path = att.path
                    )
                },
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

    fun attachFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val (name, mime) = resolveMeta(context, uri)
                val bytes = readBytes(context, uri)
                val isImage = mime.startsWith("image/", ignoreCase = true)
                val previewDataUri = if (isImage) {
                    "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else {
                    null
                }

                val (labels, ocrText) = if (isImage) analyzeImage(bytes) else (null to null)
                val payload = PromptAttachment(
                    id = UUID.randomUUID().toString(),
                    filename = name,
                    mimeType = mime,
                    path = uri.toString(), // local-only for now (no upload)
                    size = bytes.size.toLong(),
                    description = labels,
                    ocrText = ocrText,
                    previewBase64 = previewDataUri
                )
                addAttachment(payload)
            }.onFailure { it.printStackTrace() }
        }
    }

    private suspend fun analyzeImage(bytes: ByteArray): Pair<String?, String?> = runCatching {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null to null
        val image = InputImage.fromBitmap(bitmap, 0)
        val labels = deriveImageLabels(image)
        val ocrText = deriveImageOcrText(image)
        labels to ocrText
    }.getOrDefault(null to null)

    private suspend fun deriveImageLabels(image: InputImage): String? {
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
        return top.takeIf { it.isNotEmpty() }?.joinToString(", ")
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
