package com.sunflowerthu.meshcourier.domain.usecase

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.models.PacketType
import com.sunflowerthu.meshcourier.mocks.MockCryptoManager
import com.sunflowerthu.meshcourier.mocks.MockPacketQueue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SendKeyExchangeUseCaseTest {

    private val myNodeId = NodeId("my-node")
    private val recipientId = NodeId("their-node")

    private lateinit var crypto: MockCryptoManager
    private lateinit var queue: MockPacketQueue
    private lateinit var useCase: SendKeyExchangeUseCase

    @Before
    fun setUp() {
        crypto = MockCryptoManager()
        queue = MockPacketQueue()
        useCase = SendKeyExchangeUseCase(crypto, queue, NodeIdentity(myNodeId))
    }

    @Test(expected = SendKeyExchangeUseCase.OwnKeyPairMissing::class)
    fun `throws OwnKeyPairMissing when no key pair`(): Unit = runBlocking {
        crypto.hasOwnKey = false
        useCase.execute(recipientId)
    }

    @Test
    fun `enqueues KEY_EXCHANGE packet when key pair present`() = runBlocking {
        crypto.hasOwnKey = true
        useCase.execute(recipientId)
        assertEquals(1, queue.enqueued.size)
        assertEquals(PacketType.KEY_EXCHANGE, queue.enqueued[0].packetType)
    }

    @Test
    fun `packet sender and receiver are correct`() = runBlocking {
        crypto.hasOwnKey = true
        useCase.execute(recipientId)
        val packet = queue.enqueued[0]
        assertEquals(myNodeId, packet.senderNodeId)
        assertEquals(recipientId, packet.receiverNodeId)
    }

    @Test
    fun `packet payload contains own public key bytes`() = runBlocking {
        crypto.hasOwnKey = true
        val keyBytes = ByteArray(32) { it.toByte() }
        crypto.publicKeyBytes = keyBytes
        useCase.execute(recipientId)
        assertArrayEquals(keyBytes, queue.enqueued[0].payloadChunk)
    }

    @Test
    fun `packet ttl is 64`() = runBlocking {
        crypto.hasOwnKey = true
        useCase.execute(recipientId)
        assertEquals(64, queue.enqueued[0].ttl)
    }
}
