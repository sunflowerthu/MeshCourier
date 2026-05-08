package com.sunflowerthu.meshcourier.mocks

import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class MockMessageRepository : MessageRepository {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val store = mutableMapOf<UUID, Message>()

    override suspend fun save(message: Message) {
        store[message.messageId] = message
        _messages.value = store.values.toList()
    }

    override suspend fun update(message: Message) {
        store[message.messageId] = message
        _messages.value = store.values.toList()
    }

    override suspend fun getById(messageId: UUID) = store[messageId]
    override fun observeAll(): Flow<List<Message>> = _messages
    override fun observeConversation(myNodeId: NodeId, contactNodeId: NodeId): Flow<List<Message>> =
        MutableStateFlow(_messages.value.filter {
            (it.senderNodeId == myNodeId && it.receiverNodeId == contactNodeId) ||
                    (it.senderNodeId == contactNodeId && it.receiverNodeId == myNodeId)
        })
    override suspend fun getPending() = emptyList<Message>()
    override suspend fun markConversationRead(myNodeId: NodeId, contactNodeId: NodeId) {}
}
