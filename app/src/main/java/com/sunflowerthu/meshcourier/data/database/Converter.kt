package com.sunflowerthu.meshcourier.data.database

import Message
import androidx.room.TypeConverter
import com.sunflowerthu.meshcourier.data.database.entities.MessageEntity

class Converter {
    @TypeConverter
    fun messageToEntity(message: Message): MessageEntity {
        return MessageEntity(message.id, message.receiverNodeId,
            message.senderNodeId, message.content,
            message.ttl, message.hopCount, message.timestamp,
            message.status.toString())
    }
}