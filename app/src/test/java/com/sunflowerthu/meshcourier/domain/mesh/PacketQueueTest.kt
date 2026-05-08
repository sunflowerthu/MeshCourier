package com.sunflowerthu.meshcourier.domain.mesh

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.Packet
import com.sunflowerthu.meshcourier.domain.models.PacketType
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PacketQueueTest {

    private val sender = NodeId("sender")
    private val receiver = NodeId("receiver")

    private fun makePacket(type: PacketType) = Packet(
        packetId = UUID.randomUUID(),
        messageId = UUID.randomUUID(),
        senderNodeId = sender,
        receiverNodeId = receiver,
        ttl = 10,
        hopCount = 0,
        packetType = type,
        payloadChunk = byteArrayOf(0x01)
    )

    @Test
    fun `drainAll returns packets ordered by priority ACK first`() {
        val queue = PacketQueue()
        val message = makePacket(PacketType.MESSAGE)
        val ack = makePacket(PacketType.ACK)
        val hello = makePacket(PacketType.HELLO)
        val keyEx = makePacket(PacketType.KEY_EXCHANGE)

        queue.enqueue(message)
        queue.enqueue(hello)
        queue.enqueue(keyEx)
        queue.enqueue(ack)

        val drained = queue.drainAll()

        assertEquals(4, drained.size)
        assertEquals(PacketType.ACK, drained[0].packetType)
        assertEquals(PacketType.KEY_EXCHANGE, drained[1].packetType)
        assertEquals(PacketType.HELLO, drained[2].packetType)
        assertEquals(PacketType.MESSAGE, drained[3].packetType)
    }

    @Test
    fun `drainAll on empty queue returns empty list`() {
        assertTrue(PacketQueue().drainAll().isEmpty())
    }

    @Test
    fun `drainAll empties the queue`() {
        val queue = PacketQueue()
        queue.enqueue(makePacket(PacketType.ACK))
        queue.enqueue(makePacket(PacketType.MESSAGE))
        queue.drainAll()
        assertTrue(queue.isEmpty())
    }

    @Test
    fun `isEmpty and size reflect queue state`() {
        val queue = PacketQueue()
        assertTrue(queue.isEmpty())
        assertEquals(0, queue.size())

        queue.enqueue(makePacket(PacketType.MESSAGE))
        assertFalse(queue.isEmpty())
        assertEquals(1, queue.size())
    }

    @Test
    fun `dropExpired removes packets older than TTL_MS`() {
        val pastTime = System.currentTimeMillis() - PacketQueue.TTL_MS - 1000
        val queue = object : PacketQueue() {
            override fun enqueue(packet: Packet) = enqueueAt(packet, pastTime)
        }
        val packet = makePacket(PacketType.MESSAGE)
        queue.enqueue(packet)

        val expired = queue.dropExpired()

        assertEquals(1, expired.size)
        assertEquals(packet, expired[0])
        assertTrue(queue.isEmpty())
    }

    @Test
    fun `dropExpired keeps fresh packets`() {
        val queue = PacketQueue()
        queue.enqueue(makePacket(PacketType.MESSAGE))

        val expired = queue.dropExpired()

        assertTrue(expired.isEmpty())
        assertEquals(1, queue.size())
    }

    @Test
    fun `dropExpired removes only old packets leaving fresh ones`() {
        val pastTime = System.currentTimeMillis() - PacketQueue.TTL_MS - 1000
        val queue = object : PacketQueue() {
            var useFixedTime = false
            override fun enqueue(packet: Packet) {
                if (useFixedTime) enqueueAt(packet, pastTime)
                else super.enqueue(packet)
            }
        }
        val oldPacket = makePacket(PacketType.ACK).also {
            queue.useFixedTime = true
            queue.enqueue(it)
        }
        val freshPacket = makePacket(PacketType.MESSAGE).also {
            queue.useFixedTime = false
            queue.enqueue(it)
        }

        val expired = queue.dropExpired()

        assertEquals(listOf(oldPacket), expired)
        assertEquals(1, queue.size())
    }
}
