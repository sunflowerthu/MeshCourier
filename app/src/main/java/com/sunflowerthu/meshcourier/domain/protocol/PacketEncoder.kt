package com.sunflowerthu.meshcourier.domain.protocol

import com.sunflowerthu.meshcourier.domain.models.Packet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.UUID

object PacketEncoder {

    fun encode(packet: Packet): ByteArray {
        val fromBytes = packet.senderNodeId.value.toByteArray(StandardCharsets.UTF_8)
        val toBytes = packet.receiverNodeId.value.toByteArray(StandardCharsets.UTF_8)
        val payload = packet.payloadChunk

        require(fromBytes.size <= PacketFormat.MAX_NODE_ID_LENGTH)
        require(toBytes.size <= PacketFormat.MAX_NODE_ID_LENGTH)
        require(payload.size <= PacketFormat.MAX_PAYLOAD_SIZE)

        val capacity =
            PacketFormat.MIN_HEADER_SIZE +
                    fromBytes.size +
                    toBytes.size +
                    payload.size

        val buffer = ByteBuffer
            .allocate(capacity)
            .order(ByteOrder.BIG_ENDIAN)

        // header
        buffer.put(PacketFormat.VERSION)
        buffer.put(packet.packetType.ordinal.toByte())
        buffer.put(packet.ttl.toByte())
        buffer.put(packet.hopCount.toByte())

        putUuid(buffer, packet.packetId)
        putUuid(buffer, packet.messageId)

        // senderNodeId
        buffer.put(fromBytes.size.toByte())
        buffer.put(fromBytes)

        // receiverNodeId
        buffer.put(toBytes.size.toByte())
        buffer.put(toBytes)

        // payload
        buffer.putShort(payload.size.toShort())
        buffer.put(payload)

        return buffer.array()
    }

    private fun putUuid(buffer: ByteBuffer, uuid: UUID) {
        buffer.putLong(uuid.mostSignificantBits)
        buffer.putLong(uuid.leastSignificantBits)
    }
}