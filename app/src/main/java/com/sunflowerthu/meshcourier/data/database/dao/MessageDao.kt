package com.sunflowerthu.meshcourier.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sunflowerthu.meshcourier.data.database.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages")
    fun observeAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE (senderNodeId = :myId AND receiverNodeId = :contactId) OR (senderNodeId = :contactId AND receiverNodeId = :myId) ORDER BY createdAt ASC")
    fun observeConversation(myId: String, contactId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE status = 'PENDING' OR status = 'IN_TRANSIT'")
    suspend fun getPending(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity)

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getById(messageId: String): MessageEntity?

    @Update
    suspend fun update(entity: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE receiverNodeId = :myId AND senderNodeId = :contactId AND isRead = 0")
    suspend fun markAllReadInConversation(myId: String, contactId: String)
}