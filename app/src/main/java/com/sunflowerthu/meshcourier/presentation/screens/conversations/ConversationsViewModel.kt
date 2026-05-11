package com.sunflowerthu.meshcourier.presentation.screens.conversations

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.repository.ContactRepository
import com.sunflowerthu.meshcourier.domain.repository.MessageRepository
import com.sunflowerthu.meshcourier.domain.usecase.GetOwnNodeIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationItem(
    val contactNodeId: NodeId,
    val contactName: String?,
    val lastMessage: Message,
    val unreadCount: Int,
    val isLastMessageOutgoing: Boolean,
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    messageRepository: MessageRepository,
    contactRepository: ContactRepository,
    getOwnNodeIdUseCase: GetOwnNodeIdUseCase,
    private val cryptoManager: CryptoManager,
) : ViewModel() {

    private val myNodeId = getOwnNodeIdUseCase.execute()

    val conversations = combine(
        messageRepository.observeAll(),
        contactRepository.observeAll()
    ) { messages, contactNames ->
        messages.toConversations(myNodeId, contactNames)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contacts = contactRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * Парсит QR-строку формата "<nodeId>|<base64Key>" или просто "<nodeId>".
     * Если ключ присутствует — импортирует его. Возвращает Node ID или null при невалидном QR.
     */
    fun hasOwnKeyPair(): Boolean = cryptoManager.hasOwnKeyPair()

    fun parseAndImportQr(qrContent: String): String? {
        val parts = qrContent.trim().split("|", limit = 2)
        val nodeId = parts[0].trim()
        if (nodeId.isBlank()) return null
        if (parts.size == 2 && parts[1].isNotBlank()) {
            val pubKeyBytes = runCatching {
                Base64.decode(parts[1].trim(), Base64.NO_WRAP)
            }.getOrNull()
            if (pubKeyBytes != null) {
                viewModelScope.launch {
                    runCatching { cryptoManager.importPeerPublicKey(NodeId(nodeId), pubKeyBytes) }
                }
            }
        }
        return nodeId
    }

    private fun List<Message>.toConversations(
        myNodeId: NodeId,
        contactNames: Map<NodeId, String>
    ): List<ConversationItem> =
        groupBy { msg ->
            if (msg.senderNodeId == myNodeId) msg.receiverNodeId else msg.senderNodeId
        }
            .map { (contactNodeId, msgs) ->
                val lastMsg = msgs.maxBy { it.createdAt }
                ConversationItem(
                    contactNodeId = contactNodeId,
                    contactName = contactNames[contactNodeId],
                    lastMessage = lastMsg,
                    unreadCount = msgs.count { it.senderNodeId != myNodeId && !it.isRead },
                    isLastMessageOutgoing = lastMsg.senderNodeId == myNodeId
                )
            }
            .sortedByDescending { it.lastMessage.createdAt }
}
