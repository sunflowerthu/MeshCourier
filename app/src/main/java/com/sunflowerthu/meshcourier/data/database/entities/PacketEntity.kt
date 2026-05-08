package com.sunflowerthu.meshcourier.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sunflowerthu.meshcourier.domain.models.PacketType

@Entity(tableName = "packets")
data class PacketEntity(
    @PrimaryKey val packetId: String,
    val messageId: String,
    val senderNodeId: String,
    val receiverNodeId: String,
    val ttl: Int,
    val hopCount: Int,
    val packetType: PacketType,
    val payloadChunk: ByteArray,
    val enqueuedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PacketEntity) return false
        return packetId == other.packetId &&
                messageId == other.messageId &&
                senderNodeId == other.senderNodeId &&
                receiverNodeId == other.receiverNodeId &&
                ttl == other.ttl &&
                hopCount == other.hopCount &&
                packetType == other.packetType &&
                payloadChunk.contentEquals(other.payloadChunk)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + senderNodeId.hashCode()
        result = 31 * result + receiverNodeId.hashCode()
        result = 31 * result + ttl
        result = 31 * result + hopCount
        result = 31 * result + packetType.hashCode()
        result = 31 * result + payloadChunk.contentHashCode()
        return result
    }
}
