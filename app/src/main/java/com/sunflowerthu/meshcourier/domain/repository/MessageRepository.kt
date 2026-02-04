package com.sunflowerthu.meshcourier.domain.repository

import Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun save(message: Message)
    suspend fun update(message: Message)
    fun observeAll(): Flow<List<Message>>
    suspend fun getPending(): List<Message>
}