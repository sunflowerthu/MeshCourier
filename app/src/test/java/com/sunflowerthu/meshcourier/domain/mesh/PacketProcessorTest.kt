package com.sunflowerthu.meshcourier.domain.mesh

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeId.Companion.BROADCAST
import com.sunflowerthu.meshcourier.domain.models.Packet
import com.sunflowerthu.meshcourier.domain.models.PacketType
import com.sunflowerthu.meshcourier.mocks.MockSeenPacketCache
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class PacketProcessorTest {

    private val myNodeId = NodeId("my-node")
    private val peerNode = NodeId("peer-node")
    private val thirdNode = NodeId("third-node")

    private lateinit var cache: MockSeenPacketCache
    private lateinit var processor: PacketProcessor

    @Before
    fun setUp() {
        cache = MockSeenPacketCache()
        processor = PacketProcessor(myNodeId, cache)
    }

    private fun makePacket(
        sender: NodeId = peerNode,
        receiver: NodeId = myNodeId,
        ttl: Int = 5,
        type: PacketType = PacketType.MESSAGE
    ) = Packet(
        packetId = UUID.randomUUID(),
        messageId = UUID.randomUUID(),
        senderNodeId = sender,
        receiverNodeId = receiver,
        ttl = ttl,
        hopCount = 0,
        packetType = type,
        payloadChunk = byteArrayOf(0x01)
    )

    @Test
    fun `packet addressed to own node is delivered`() {
        val packet = makePacket(receiver = myNodeId)
        val action = processor.process(packet)
        assertTrue(action is PacketAction.Deliver)
        assertEquals(packet, (action as PacketAction.Deliver).packet)
    }

    @Test
    fun `broadcast packet is delivered`() {
        val packet = makePacket(receiver = BROADCAST, sender = peerNode)
        assertTrue(processor.process(packet) is PacketAction.Deliver)
    }

    @Test
    fun `duplicate packet is dropped`() {
        val packet = makePacket()
        processor.process(packet)
        assertSame(PacketAction.Drop, processor.process(packet))
    }

    @Test
    fun `packet for another node with ttl is forwarded`() {
        val packet = makePacket(sender = peerNode, receiver = thirdNode, ttl = 5)
        val action = processor.process(packet)
        assertTrue(action is PacketAction.Forward)
        val forwarded = (action as PacketAction.Forward).packet
        assertEquals(4, forwarded.ttl)
        assertEquals(1, forwarded.hopCount)
    }

    @Test
    fun `packet for another node with ttl zero is dropped`() {
        val packet = makePacket(sender = peerNode, receiver = thirdNode, ttl = 0)
        assertSame(PacketAction.Drop, processor.process(packet))
    }

    @Test
    fun `processed packet is marked seen`() {
        val packet = makePacket()
        processor.process(packet)
        assertTrue(cache.hasSeen(packet.packetId))
    }

    @Test
    fun `broadcast packet with ttl zero is still delivered`() {
        // Broadcast receiver check happens before TTL check
        val packet = makePacket(receiver = BROADCAST, sender = peerNode, ttl = 0, type = PacketType.HELLO)
        assertTrue(processor.process(packet) is PacketAction.Deliver)
    }

    @Test
    fun `all packet types addressed to own node are delivered`() {
        for (type in PacketType.entries) {
            val packet = makePacket(receiver = myNodeId, type = type)
            assertTrue("Expected Deliver for $type", processor.process(packet) is PacketAction.Deliver)
        }
    }
}
