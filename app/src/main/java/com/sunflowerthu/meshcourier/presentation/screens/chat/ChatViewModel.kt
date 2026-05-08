package com.sunflowerthu.meshcourier.presentation.screens.chat

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflowerthu.meshcourier.R
import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.MessageStatus
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.protocol.PacketFormat
import com.sunflowerthu.meshcourier.domain.repository.ContactRepository
import com.sunflowerthu.meshcourier.domain.repository.MessageRepository
import com.sunflowerthu.meshcourier.domain.repository.PeerKeyRepository
import com.sunflowerthu.meshcourier.domain.usecase.GetOwnNodeIdUseCase
import com.sunflowerthu.meshcourier.domain.usecase.SendKeyExchangeUseCase
import com.sunflowerthu.meshcourier.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import androidx.core.graphics.scale

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val peerKeyRepository: PeerKeyRepository,
    getOwnNodeIdUseCase: GetOwnNodeIdUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendKeyExchangeUseCase: SendKeyExchangeUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val myNodeId = getOwnNodeIdUseCase.execute()
    val contactNodeId = NodeId(checkNotNull(savedStateHandle["contactNodeId"]))

    val messages = messageRepository.observeConversation(myNodeId, contactNodeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contactName = contactRepository.observeDisplayName(contactNodeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Реактивно переключается в true когда приходит ответный ключ от собеседника
    val encryptionAvailable = peerKeyRepository.observeKnownNodeIds()
        .map { contactNodeId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { messageRepository.markConversationRead(myNodeId, contactNodeId) }
        }
        viewModelScope.launch {
            runCatching { sendKeyExchangeUseCase.execute(contactNodeId) }
        }
    }

    fun renameContact(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { contactRepository.setDisplayName(contactNodeId, name.trim().take(50)) }
        }
    }

    fun deleteContact() {
        viewModelScope.launch {
            runCatching { contactRepository.delete(contactNodeId) }
        }
    }

    fun markRead() {
        viewModelScope.launch {
            runCatching { messageRepository.markConversationRead(myNodeId, contactNodeId) }
        }
    }

    fun sendKeyExchange() {
        viewModelScope.launch {
            runCatching { sendKeyExchangeUseCase.execute(contactNodeId) }
                .onSuccess { _events.tryEmit(context.getString(R.string.chat_event_keys_request_sent)) }
                .onFailure { _events.tryEmit(context.getString(R.string.chat_event_keys_request_failed, it.message ?: "")) }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val bytes = text.trim().toByteArray(Charsets.UTF_8)
        if (bytes.size > PacketFormat.MAX_PAYLOAD_SIZE) return
        viewModelScope.launch {
            val message = Message(
                messageId = UUID.randomUUID(),
                senderNodeId = myNodeId,
                receiverNodeId = contactNodeId,
                payload = bytes,
                status = MessageStatus.PENDING
            )
            runCatching { sendMessageUseCase.execute(message) }
        }
    }

    fun sendFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val cr = context.contentResolver
                val mimeType = cr.getType(uri) ?: "application/octet-stream"
                val fileName = resolveFileName(uri) ?: context.getString(R.string.chat_file_default)
                var bytes = cr.openInputStream(uri)?.use { it.readBytes() }
                    ?: error(context.getString(R.string.chat_event_file_read_error))

                if (mimeType.startsWith("image/")) {
                    bytes = compressImage(bytes)
                }

                val encodedSize = 5 + mimeType.toByteArray().size + fileName.toByteArray().size + bytes.size
                if (encodedSize > PacketFormat.MAX_PAYLOAD_SIZE) {
                    _events.tryEmit(context.getString(R.string.chat_event_file_too_large, PacketFormat.MAX_PAYLOAD_SIZE / 1024))
                    return@runCatching
                }

                val message = Message(
                    messageId = UUID.randomUUID(),
                    senderNodeId = myNodeId,
                    receiverNodeId = contactNodeId,
                    payload = bytes,
                    mimeType = mimeType,
                    fileName = fileName,
                    status = MessageStatus.PENDING
                )
                sendMessageUseCase.execute(message)
            }.onFailure { _events.tryEmit(context.getString(R.string.chat_event_file_send_error, it.message ?: "")) }
        }
    }

    fun openAttachment(message: Message) {
        if (message.mimeType == null) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
                val safeName = (message.fileName ?: "file")
                    .replace(Regex("[^A-Za-zА-Яа-я0-9._-]"), "_")
                    .take(100)
                val file = File(dir, "${message.messageId}_$safeName")
                if (!file.exists() || file.length() != message.payload.size.toLong()) {
                    file.writeBytes(message.payload)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, message.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    _events.tryEmit(context.getString(R.string.chat_event_no_app_for_file))
                }
            }.onFailure { _events.tryEmit(context.getString(R.string.chat_event_open_file_error, it.message ?: "")) }
        }
    }

    private fun resolveFileName(uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
        }

    private fun compressImage(bytes: ByteArray): ByteArray {
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
        val maxDim = 400
        val scale = minOf(1f, maxDim.toFloat() / maxOf(original.width, original.height))
        val scaled = if (scale < 1f) {
            original.scale((original.width * scale).toInt(), (original.height * scale).toInt())
        } else original
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 60, out)
        return out.toByteArray()
    }
}
