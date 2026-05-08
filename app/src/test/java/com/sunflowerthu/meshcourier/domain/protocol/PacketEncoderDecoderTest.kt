package com.sunflowerthu.meshcourier.domain.protocol

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.Packet
import com.sunflowerthu.meshcourier.domain.models.PacketType
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PacketEncoderDecoderTest {

    private val sender = NodeId("node-A")
    private val receiver = NodeId("node-B")

    private fun makePacket(
        type: PacketType = PacketType.MESSAGE,
        ttl: Int = 10,
        hopCount: Int = 2,
        payload: ByteArray = "hello".toByteArray()
    ) = Packet(
        packetId = UUID.randomUUID(),
        messageId = UUID.randomUUID(),
        senderNodeId = sender,
        receiverNodeId = receiver,
        ttl = ttl,
        hopCount = hopCount,
        packetType = type,
        payloadChunk = payload
    )

    @Test
    fun `encode then decode returns identical packet`() {
        val original = makePacket()
        val decoded = PacketDecoder.decode(PacketEncoder.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `round trip preserves all packet types`() {
        for (type in PacketType.entries) {
            val packet = makePacket(type = type)
            val decoded = PacketDecoder.decode(PacketEncoder.encode(packet))
            assertEquals(type, decoded.packetType)
        }
    }

    @Test
    fun `round trip with broadcast receiver`() {
        val packet = Packet(
            packetId = UUID.randomUUID(),
            messageId = UUID.randomUUID(),
            senderNodeId = NodeId("node-A"),
            receiverNodeId = NodeId.BROADCAST,
            ttl = 1,
            hopCount = 0,
            packetType = PacketType.HELLO,
            payloadChunk = byteArrayOf(0x01)
        )
        val decoded = PacketDecoder.decode(PacketEncoder.encode(packet))
        assertEquals(packet, decoded)
    }

    @Test
    fun `round trip preserves ttl zero and hopCount 255`() {
        val packet = makePacket(ttl = 0, hopCount = 255, payload = byteArrayOf(0x42))
        val decoded = PacketDecoder.decode(PacketEncoder.encode(packet))
        assertEquals(0, decoded.ttl)
        assertEquals(255, decoded.hopCount)
    }

    @Test
    fun `round trip with large payload`() {
        val bigPayload = ByteArray(1000) { it.toByte() }
        val decoded = PacketDecoder.decode(PacketEncoder.encode(makePacket(payload = bigPayload)))
        assertArrayEquals(bigPayload, decoded.payloadChunk)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode throws on buffer too short`() {
        PacketDecoder.decode(ByteArray(10))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode throws on wrong version`() {
        val bytes = PacketEncoder.encode(makePacket())
        bytes[0] = 0x99.toByte()
        PacketDecoder.decode(bytes)
    }

    @Test(expected = UnknownTypeOfPacketException::class)
    fun `decode throws on unknown packet type`() {
        val bytes = PacketEncoder.encode(makePacket())
        bytes[1] = 0x7F.toByte()
        PacketDecoder.decode(bytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `encoder throws when payload exceeds max size`() {
        val packet = Packet(
            packetId = UUID.randomUUID(),
            messageId = UUID.randomUUID(),
            senderNodeId = sender,
            receiverNodeId = receiver,
            ttl = 1,
            hopCount = 0,
            packetType = PacketType.MESSAGE,
            payloadChunk = ByteArray(PacketFormat.MAX_PAYLOAD_SIZE + 1)
        )
        PacketEncoder.encode(packet)
    }
}
