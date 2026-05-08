package com.sunflowerthu.meshcourier.domain.models

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PacketTest {

    private val a = NodeId("node-a")
    private val b = NodeId("node-b")

    private fun base() = Packet(
        packetId = UUID.randomUUID(),
        messageId = UUID.randomUUID(),
        senderNodeId = a,
        receiverNodeId = b,
        ttl = 5,
        hopCount = 0,
        packetType = PacketType.MESSAGE,
        payloadChunk = byteArrayOf(0x42)
    )

    @Test
    fun `forwarded decrements ttl and increments hopCount`() {
        val forwarded = base().forwarded()
        assertEquals(4, forwarded.ttl)
        assertEquals(1, forwarded.hopCount)
    }

    @Test
    fun `forwarded preserves all other fields`() {
        val packet = base()
        val forwarded = packet.forwarded()
        assertEquals(packet.packetId, forwarded.packetId)
        assertEquals(packet.messageId, forwarded.messageId)
        assertEquals(packet.senderNodeId, forwarded.senderNodeId)
        assertEquals(packet.receiverNodeId, forwarded.receiverNodeId)
        assertEquals(packet.packetType, forwarded.packetType)
        assertArrayEquals(packet.payloadChunk, forwarded.payloadChunk)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `forwarded with ttl zero throws`() {
        base().copy(ttl = 0).forwarded()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative ttl throws in constructor`() {
        base().copy(ttl = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative hopCount throws in constructor`() {
        base().copy(hopCount = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty payload throws in constructor`() {
        Packet(
            packetId = UUID.randomUUID(),
            messageId = UUID.randomUUID(),
            senderNodeId = a,
            receiverNodeId = b,
            ttl = 1,
            hopCount = 0,
            packetType = PacketType.MESSAGE,
            payloadChunk = ByteArray(0)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `same sender and receiver throws`() {
        Packet(
            packetId = UUID.randomUUID(),
            messageId = UUID.randomUUID(),
            senderNodeId = a,
            receiverNodeId = a,
            ttl = 1,
            hopCount = 0,
            packetType = PacketType.MESSAGE,
            payloadChunk = byteArrayOf(0x01)
        )
    }
}
