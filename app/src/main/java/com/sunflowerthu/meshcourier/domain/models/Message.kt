package com.sunflowerthu.meshcourier.domain.models

import java.util.Date
import java.util.UUID

data class Message(
    val messageId : UUID,
    val senderNodeId : NodeId,
    val receiverNodeId : NodeId,
    val payload : ByteArray,
    val ttl : Int = 64,
    val hopCount: Int = 0,
    val createdAt: Long = Date().time,
    val status : MessageStatus,
    val isRead: Boolean = false,
    val mimeType: String? = null,
    val fileName: String? = null,
) {
    init {
        require(ttl >= 0)
        require(senderNodeId != receiverNodeId)
        require(payload.isNotEmpty())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (ttl != other.ttl) return false
        if (hopCount != other.hopCount) return false
        if (createdAt != other.createdAt) return false
        if (messageId != other.messageId) return false
        if (senderNodeId != other.senderNodeId) return false
        if (receiverNodeId != other.receiverNodeId) return false
        if (!payload.contentEquals(other.payload)) return false
        if (status != other.status) return false
        if (isRead != other.isRead) return false
        if (mimeType != other.mimeType) return false
        if (fileName != other.fileName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ttl
        result = 31 * result + hopCount
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + senderNodeId.hashCode()
        result = 31 * result + receiverNodeId.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + isRead.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }
}