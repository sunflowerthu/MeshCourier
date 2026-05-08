package com.sunflowerthu.meshcourier.domain.models

import java.util.UUID

data class Packet(
    val packetId : UUID,
    val messageId : UUID,
    val senderNodeId : NodeId,
    val receiverNodeId : NodeId,
    val ttl : Int,
    val hopCount : Int,
    val packetType: PacketType,
    val payloadChunk : ByteArray
) {
    init {
        require(ttl >= 0)
        require(hopCount >= 0)
        require(payloadChunk.isNotEmpty())
        require(senderNodeId != receiverNodeId)
    }

    fun forwarded(): Packet {
        require(ttl > 0)
        return copy(
            ttl = ttl - 1,
            hopCount = hopCount + 1
        )
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Packet

        if (ttl != other.ttl) return false
        if (hopCount != other.hopCount) return false
        if (packetId != other.packetId) return false
        if (messageId != other.messageId) return false
        if (senderNodeId != other.senderNodeId) return false
        if (receiverNodeId != other.receiverNodeId) return false
        if (packetType != other.packetType) return false
        if (!payloadChunk.contentEquals(other.payloadChunk)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ttl
        result = 31 * result + hopCount
        result = 31 * result + packetId.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + senderNodeId.hashCode()
        result = 31 * result + receiverNodeId.hashCode()
        result = 31 * result + packetType.hashCode()
        result = 31 * result + payloadChunk.contentHashCode()
        return result
    }
}