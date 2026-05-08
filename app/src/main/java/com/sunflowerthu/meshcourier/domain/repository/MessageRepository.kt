package com.sunflowerthu.meshcourier.domain.repository

import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.NodeId
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface MessageRepository {
    suspend fun save(message: Message)
    suspend fun update(message: Message)
    suspend fun getById(messageId: UUID): Message?
    fun observeAll(): Flow<List<Message>>
    fun observeConversation(myNodeId: NodeId, contactNodeId: NodeId): Flow<List<Message>>
    suspend fun getPending(): List<Message>
    suspend fun markConversationRead(myNodeId: NodeId, contactNodeId: NodeId)
}