package com.sunflowerthu.meshcourier.domain.protocol

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.Packet
import com.sunflowerthu.meshcourier.domain.models.PacketType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.UUID

object PacketDecoder {

    fun decode(bytes: ByteArray): Packet {
        val buffer = ByteBuffer
            .wrap(bytes)
            .order(ByteOrder.BIG_ENDIAN)

        require(buffer.remaining() >= PacketFormat.MIN_HEADER_SIZE) {
            "Слишком маленький размер пакета"
        }

        val version = buffer.get()
        require(version == PacketFormat.VERSION) {
            "Неподдерживаемая версия пакета: $version"
        }

        val typeOrdinal = buffer.get().toInt()
        val type = PacketType.entries.getOrNull(typeOrdinal)
            ?: throw UnknownTypeOfPacketException("Неизвестный тип пакета: $typeOrdinal")

        val ttl = buffer.get().toInt() and 0xFF
        val hopCount = buffer.get().toInt() and 0xFF

        val packetId = readUuid(buffer)
        val messageId = readUuid(buffer)

        val senderNodeId = readNodeId(buffer)
        val receiverNodeId = readNodeId(buffer)

        val payloadLength = buffer.short.toInt() and 0xFFFF
        require(payloadLength <= PacketFormat.MAX_PAYLOAD_SIZE)
        require(payloadLength <= buffer.remaining()) {
            "Длина payload превышает длину буфера"
        }

        val payload = ByteArray(payloadLength)
        buffer.get(payload)

        return Packet(
            packetId = packetId,
            messageId = messageId,
            senderNodeId = senderNodeId,
            receiverNodeId = receiverNodeId,
            ttl = ttl,
            hopCount = hopCount,
            packetType = type,
            payloadChunk = payload,
        )
    }

    private fun readUuid(buffer: ByteBuffer): UUID {
        val msb = buffer.long
        val lsb = buffer.long
        return UUID(msb, lsb)
    }

    private fun readNodeId(buffer: ByteBuffer): NodeId {
        val length = buffer.get().toInt() and 0xFF
        require(length > 0)
        require(length <= PacketFormat.MAX_NODE_ID_LENGTH)
        require(length <= buffer.remaining())

        val bytes = ByteArray(length)
        buffer.get(bytes)

        return NodeId(String(bytes, StandardCharsets.UTF_8))
    }
}