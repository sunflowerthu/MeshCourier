package com.sunflowerthu.meshcourier.domain.usecase

import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.MessageStatus
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.models.Packet
import com.sunflowerthu.meshcourier.domain.models.PacketType
import com.sunflowerthu.meshcourier.mocks.MockCryptoManager
import com.sunflowerthu.meshcourier.mocks.MockMessageRepository
import com.sunflowerthu.meshcourier.mocks.MockNearbyNodesRepository
import com.sunflowerthu.meshcourier.mocks.MockPacketQueue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ReceiveMessageUseCaseTest {

    private val myNode = NodeId("my-node")
    private val peerNode = NodeId("peer-node")
    private val otherNode = NodeId("other-node")

    private lateinit var crypto: MockCryptoManager
    private lateinit var queue: MockPacketQueue
    private lateinit var repo: MockMessageRepository
    private lateinit var nearby: MockNearbyNodesRepository
    private lateinit var useCase: ReceiveMessageUseCase

    @Before
    fun setUp() {
        crypto = MockCryptoManager()
        queue = MockPacketQueue()
        repo = MockMessageRepository()
        nearby = MockNearbyNodesRepository()
        useCase = ReceiveMessageUseCase(crypto, queue, repo, nearby, NodeIdentity(myNode))
    }

    private fun packet(
        sender: NodeId = peerNode,
        receiver: NodeId = myNode,
        payload: ByteArray,
        type: PacketType = PacketType.MESSAGE,
        messageId: UUID = UUID.randomUUID()
    ) = Packet(
        packetId = UUID.randomUUID(),
        messageId = messageId,
        senderNodeId = sender,
        receiverNodeId = receiver,
        ttl = 10,
        hopCount = 0,
        packetType = type,
        payloadChunk = payload
    )

    // --- MESSAGE ---

    @Test
    fun `incoming message saved as PROCESSING then updated to DELIVERED`() = runBlocking {
        useCase.execute(packet(payload = byteArrayOf(0x00, 0x41)))
        assertEquals(MessageStatus.PROCESSING, repo.saved[0].status)
        assertEquals(MessageStatus.DELIVERED, repo.updated[0].status)
    }

    @Test
    fun `message with 0x00 prefix - delivered payload has prefix stripped`() = runBlocking {
        val text = "hello".toByteArray()
        useCase.execute(packet(payload = byteArrayOf(0x00) + text))
        assertArrayEquals(text, repo.updated[0].payload)
    }

    @Test
    fun `message with 0x01 prefix and 64-byte signature - payload stripped`() = runBlocking {
        val text = "world".toByteArray()
        val sig = ByteArray(64) { 0x55.toByte() }
        crypto.peerKeys[peerNode] = ByteArray(1)
        crypto.verifyResult = true
        useCase.execute(packet(payload = byteArrayOf(0x01) + text + sig))
        assertArrayEquals(text, repo.updated[0].payload)
    }

    @Test
    fun `message with no flag byte passed through as-is`() = runBlocking {
        val raw = byteArrayOf(0x42, 0x43, 0x44)
        useCase.execute(packet(payload = raw))
        assertArrayEquals(raw, repo.updated[0].payload)
    }

    @Test
    fun `message with peer key - payload is decrypted before parsing`() = runBlocking {
        val decrypted = byteArrayOf(0x00, 0x41, 0x42)
        crypto.peerKeys[peerNode] = ByteArray(1)
        crypto.decryptTransform = { decrypted }
        useCase.execute(packet(payload = byteArrayOf(0x99.toByte(), 0x88.toByte())))
        // After decrypt: [0x00, 0x41, 0x42] → strip prefix → [0x41, 0x42]
        assertArrayEquals(byteArrayOf(0x41, 0x42), repo.updated[0].payload)
    }

    @Test
    fun `message with attachment encoding - mimeType and fileName decoded`() = runBlocking {
        val mimeType = "image/png"
        val fileName = "photo.png"
        val data = byteArrayOf(0x01, 0x02)
        val mimeBytes = mimeType.toByteArray()
        val nameBytes = fileName.toByteArray()
        val attachment = byteArrayOf(
            SendMessageUseCase.ATTACHMENT_MARKER,
            (mimeBytes.size shr 8).toByte(), (mimeBytes.size and 0xFF).toByte(),
            (nameBytes.size shr 8).toByte(), (nameBytes.size and 0xFF).toByte()
        ) + mimeBytes + nameBytes + data
        useCase.execute(packet(payload = byteArrayOf(0x00) + attachment))
        val updated = repo.updated[0]
        assertEquals(mimeType, updated.mimeType)
        assertEquals(fileName, updated.fileName)
        assertArrayEquals(data, updated.payload)
    }

    @Test
    fun `ACK is enqueued after receiving message`() = runBlocking {
        useCase.execute(packet(payload = byteArrayOf(0x00, 0x01)))
        val acks = queue.enqueued.filter { it.packetType == PacketType.ACK }
        assertEquals(1, acks.size)
        assertEquals(myNode, acks[0].senderNodeId)
        assertEquals(peerNode, acks[0].receiverNodeId)
    }

    // --- KEY_EXCHANGE ---

    @Test
    fun `handleKeyExchange imports peer public key`() = runBlocking {
        val keyBytes = ByteArray(32) { it.toByte() }
        useCase.execute(packet(payload = keyBytes, type = PacketType.KEY_EXCHANGE))
        assertArrayEquals(keyBytes, crypto.peerKeys[peerNode])
    }

    @Test
    fun `handleKeyExchange responds with own key when key pair present`() = runBlocking {
        crypto.hasOwnKey = true
        useCase.execute(packet(payload = ByteArray(32), type = PacketType.KEY_EXCHANGE))
        val responses = queue.enqueued.filter { it.packetType == PacketType.KEY_EXCHANGE }
        assertEquals(1, responses.size)
        assertEquals(myNode, responses[0].senderNodeId)
        assertEquals(peerNode, responses[0].receiverNodeId)
    }

    @Test
    fun `handleKeyExchange does not respond twice to same sender`() = runBlocking {
        crypto.hasOwnKey = true
        val p = packet(payload = ByteArray(32), type = PacketType.KEY_EXCHANGE)
        useCase.execute(p)
        useCase.execute(p.copy(packetId = UUID.randomUUID()))
        val responses = queue.enqueued.filter { it.packetType == PacketType.KEY_EXCHANGE }
        assertEquals(1, responses.size)
    }

    @Test
    fun `handleKeyExchange does not respond without own key pair`() = runBlocking {
        crypto.hasOwnKey = false
        useCase.execute(packet(payload = ByteArray(32), type = PacketType.KEY_EXCHANGE))
        assertTrue(queue.enqueued.filter { it.packetType == PacketType.KEY_EXCHANGE }.isEmpty())
    }

    // --- ACK ---

    @Test
    fun `handleAck updates matching message status to DELIVERED`() = runBlocking {
        val msgId = UUID.randomUUID()
        repo.save(
            Message(
                messageId = msgId,
                senderNodeId = myNode,
                receiverNodeId = peerNode,
                payload = byteArrayOf(0x01),
                status = MessageStatus.PENDING
            )
        )
        useCase.execute(
            Packet(
                packetId = UUID.randomUUID(),
                messageId = msgId,
                senderNodeId = peerNode,
                receiverNodeId = myNode,
                ttl = 64,
                hopCount = 0,
                packetType = PacketType.ACK,
                payloadChunk = byteArrayOf(0x01)
            )
        )
        assertEquals(MessageStatus.DELIVERED, repo.updated[0].status)
    }

    @Test
    fun `handleAck with unknown messageId does not crash`() = runBlocking {
        useCase.execute(
            Packet(
                packetId = UUID.randomUUID(),
                messageId = UUID.randomUUID(),
                senderNodeId = peerNode,
                receiverNodeId = myNode,
                ttl = 64,
                hopCount = 0,
                packetType = PacketType.ACK,
                payloadChunk = byteArrayOf(0x01)
            )
        )
        assertTrue(repo.updated.isEmpty())
    }

    // --- HELLO ---

    @Test
    fun `handleHello adds new node to nearby repository`() = runBlocking {
        useCase.execute(packet(type = PacketType.HELLO, payload = byteArrayOf(0x01)))
        assertTrue(nearby.nodes.value.contains(peerNode))
    }

    @Test
    fun `handleHello responds with own hello for new node`() = runBlocking {
        useCase.execute(packet(type = PacketType.HELLO, payload = byteArrayOf(0x01)))
        val hellos = queue.enqueued.filter { it.packetType == PacketType.HELLO }
        assertEquals(1, hellos.size)
        assertEquals(myNode, hellos[0].senderNodeId)
        assertEquals(peerNode, hellos[0].receiverNodeId)
    }

    @Test
    fun `handleHello does not respond twice to already known node`() = runBlocking {
        val p = packet(type = PacketType.HELLO, payload = byteArrayOf(0x01))
        useCase.execute(p)
        useCase.execute(p.copy(packetId = UUID.randomUUID()))
        val hellos = queue.enqueued.filter { it.packetType == PacketType.HELLO }
        assertEquals(1, hellos.size)
    }

    @Test
    fun `handleHello ignores packet from own node`() = runBlocking {
        val p = Packet(
            packetId = UUID.randomUUID(),
            messageId = UUID.randomUUID(),
            senderNodeId = myNode,
            receiverNodeId = otherNode,
            ttl = 1,
            hopCount = 0,
            packetType = PacketType.HELLO,
            payloadChunk = byteArrayOf(0x01)
        )
        useCase.execute(p)
        assertTrue(queue.enqueued.filter { it.packetType == PacketType.HELLO }.isEmpty())
        assertFalse(nearby.nodes.value.contains(myNode))
    }
}
