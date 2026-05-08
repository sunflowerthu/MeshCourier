package com.sunflowerthu.meshcourier.mocks

import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class MockMessageRepository : MessageRepository {
    val saved = mutableListOf<Message>()
    val updated = mutableListOf<Message>()
    private val store = mutableMapOf<UUID, Message>()
    private val _all = MutableStateFlow<List<Message>>(emptyList())

    override suspend fun save(message: Message) {
        saved.add(message)
        store[message.messageId] = message
        _all.value = store.values.toList()
    }

    override suspend fun update(message: Message) {
        updated.add(message)
        store[message.messageId] = message
        _all.value = store.values.toList()
    }

    override suspend fun getById(messageId: UUID) = store[messageId]
    override fun observeAll(): Flow<List<Message>> = _all
    override fun observeConversation(myNodeId: NodeId, contactNodeId: NodeId): Flow<List<Message>> =
        MutableStateFlow(_all.value.filter {
            (it.senderNodeId == myNodeId && it.receiverNodeId == contactNodeId) ||
                    (it.senderNodeId == contactNodeId && it.receiverNodeId == myNodeId)
        })
    override suspend fun getPending() = emptyList<Message>()
    override suspend fun markConversationRead(myNodeId: NodeId, contactNodeId: NodeId) {}
}
