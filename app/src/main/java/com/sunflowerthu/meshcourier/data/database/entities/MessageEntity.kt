package com.sunflowerthu.meshcourier.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sunflowerthu.meshcourier.domain.models.MessageStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val senderNodeId: String,
    val receiverNodeId: String,
    val payload: ByteArray,
    val ttl: Int,
    @ColumnInfo(defaultValue = "0") val hopCount: Int = 0,
    val createdAt: Long,
    val status: MessageStatus,
    @ColumnInfo(defaultValue = "0") val isRead: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val mimeType: String? = null,
    @ColumnInfo(defaultValue = "NULL") val fileName: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageEntity) return false
        return messageId == other.messageId &&
            senderNodeId == other.senderNodeId &&
            receiverNodeId == other.receiverNodeId &&
            payload.contentEquals(other.payload) &&
            ttl == other.ttl &&
            createdAt == other.createdAt &&
            status == other.status &&
            isRead == other.isRead
    }

    override fun hashCode(): Int {
        var result = messageId.hashCode()
        result = 31 * result + senderNodeId.hashCode()
        result = 31 * result + receiverNodeId.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + ttl
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + isRead.hashCode()
        return result
    }
}
