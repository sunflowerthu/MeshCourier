package com.sunflowerthu.meshcourier.domain.usecase

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.models.PacketType
import com.sunflowerthu.meshcourier.mocks.MockPacketQueue
import org.junit.Assert.*
import org.junit.Test

class SendHelloUseCaseTest {

    private val myNodeId = NodeId("my-node")
    private val queue = MockPacketQueue()
    private val useCase = SendHelloUseCase(queue, NodeIdentity(myNodeId))

    @Test
    fun `execute enqueues a HELLO packet`() {
        useCase.execute()
        assertEquals(1, queue.enqueued.size)
        assertEquals(PacketType.HELLO, queue.enqueued[0].packetType)
    }

    @Test
    fun `hello packet sender is own node`() {
        useCase.execute()
        assertEquals(myNodeId, queue.enqueued[0].senderNodeId)
    }

    @Test
    fun `hello packet receiver is BROADCAST`() {
        useCase.execute()
        assertEquals(NodeId.BROADCAST, queue.enqueued[0].receiverNodeId)
    }

    @Test
    fun `hello packet has ttl of 1`() {
        useCase.execute()
        assertEquals(1, queue.enqueued[0].ttl)
    }
}
