package com.sunflowerthu.meshcourier.data.repository

import com.sunflowerthu.meshcourier.data.database.dao.MessageDao
import com.sunflowerthu.meshcourier.data.database.entities.MessageEntity
import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(private val dao: MessageDao) : MessageRepository {

    override suspend fun save(message: Message) = dao.insert(message.toEntity())

    override suspend fun update(message: Message) = dao.update(message.toEntity())

    override suspend fun getById(messageId: UUID): Message? =
        dao.getById(messageId.toString())?.toDomain()

    override fun observeAll(): Flow<List<Message>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    override fun observeConversation(myNodeId: NodeId, contactNodeId: NodeId): Flow<List<Message>> =
        dao.observeConversation(myNodeId.value, contactNodeId.value).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun getPending(): List<Message> = dao.getPending().map { it.toDomain() }

    override suspend fun markConversationRead(myNodeId: NodeId, contactNodeId: NodeId) =
        dao.markAllReadInConversation(myNodeId.value, contactNodeId.value)

    private fun Message.toEntity() = MessageEntity(
        messageId = messageId.toString(),
        senderNodeId = senderNodeId.value,
        receiverNodeId = receiverNodeId.value,
        payload = payload,
        ttl = ttl,
        hopCount = hopCount,
        createdAt = createdAt,
        status = status,
        isRead = isRead,
        mimeType = mimeType,
        fileName = fileName,
    )

    private fun MessageEntity.toDomain() = Message(
        messageId = UUID.fromString(messageId),
        senderNodeId = NodeId(senderNodeId),
        receiverNodeId = NodeId(receiverNodeId),
        payload = payload,
        ttl = ttl,
        hopCount = hopCount,
        createdAt = createdAt,
        status = status,
        isRead = isRead,
        mimeType = mimeType,
        fileName = fileName,
    )
}
