package com.sunflowerthu.meshcourier.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id : String,
    val senderNodeId : String,
    val receiverNodeId : String,
    val content : String,
    var ttl : Int,
    var hopCount : Int,
    val timestamp: Long,
    val status : String
)
