package com.sunflowerthu.meshcourier.data.repository

import Message
import com.sunflowerthu.meshcourier.data.database.dao.MessageDao
import com.sunflowerthu.meshcourier.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow

class MessageRepositoryImpl(private val dao: MessageDao) : MessageRepository {
    override suspend fun save(message: Message) {
        dao.insert(message)
    }

    override suspend fun update(message: Message) {
        dao.update(message)
    }

    override fun observeAll(): Flow<List<Message>> {
        return dao.observeAll()
    }

    override suspend fun getPending(): List<Message> {
        return dao.getPending()
    }
}