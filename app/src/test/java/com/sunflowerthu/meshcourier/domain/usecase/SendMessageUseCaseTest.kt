package com.sunflowerthu.meshcourier.domain.usecase

import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.MessageStatus
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.PacketType
import com.sunflowerthu.meshcourier.mocks.MockCryptoManager
import com.sunflowerthu.meshcourier.mocks.MockMessageRepository
import com.sunflowerthu.meshcourier.mocks.MockPacketQueue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SendMessageUseCaseTest {

    private val sender = NodeId("sender")
    private val receiver = NodeId("receiver")

    private lateinit var crypto: MockCryptoManager
    private lateinit var queue: MockPacketQueue
    private lateinit var repo: MockMessageRepository
    private lateinit var useCase: SendMessageUseCase

    @Before
    fun setUp() {
        crypto = MockCryptoManager()
        queue = MockPacketQueue()
        repo = MockMessageRepository()
        useCase = SendMessageUseCase(crypto, queue, repo)
    }

    private fun makeMessage(
        payload: ByteArray = "hello".toByteArray(),
        mimeType: String? = null,
        fileName: String? = null
    ) = Message(
        messageId = UUID.randomUUID(),
        senderNodeId = sender,
        receiverNodeId = receiver,
        payload = payload,
        status = MessageStatus.PENDING,
        mimeType = mimeType,
        fileName = fileName
    )

    @Test
    fun `message is saved to repository before sending`() = runBlocking {
        val msg = makeMessage()
        useCase.execute(msg)
        assertEquals(1, repo.saved.size)
        assertEquals(msg.messageId, repo.saved[0].messageId)
    }

    @Test
    fun `enqueued packet has MESSAGE type`() = runBlocking {
        useCase.execute(makeMessage())
        assertEquals(PacketType.MESSAGE, queue.enqueued[0].packetType)
    }

    @Test
    fun `enqueued packet messageId matches message`() = runBlocking {
        val msg = makeMessage()
        useCase.execute(msg)
        assertEquals(msg.messageId, queue.enqueued[0].messageId)
    }

    @Test
    fun `no key pair no peer key - payload starts with 0x00 flag`() = runBlocking {
        crypto.hasOwnKey = false
        useCase.execute(makeMessage())
        assertEquals(0x00.toByte(), queue.enqueued[0].payloadChunk[0])
    }

    @Test
    fun `no key pair no peer key - plaintext follows flag byte`() = runBlocking {
        crypto.hasOwnKey = false
        val text = "hello world".toByteArray()
        useCase.execute(makeMessage(payload = text))
        val payload = queue.enqueued[0].payloadChunk
        assertArrayEquals(text, payload.copyOfRange(1, payload.size))
    }

    @Test
    fun `with key pair - signed payload starts with 0x01 flag`() = runBlocking {
        crypto.hasOwnKey = true
        useCase.execute(makeMessage())
        assertEquals(0x01.toByte(), queue.enqueued[0].payloadChunk[0])
    }

    @Test
    fun `with key pair - signed payload contains plaintext and 64-byte signature`() = runBlocking {
        crypto.hasOwnKey = true
        val sig = ByteArray(64) { it.toByte() }
        crypto.signResult = sig
        val text = "abc".toByteArray()
        useCase.execute(makeMessage(payload = text))
        val payload = queue.enqueued[0].payloadChunk
        // [0x01][text][sig64]
        assertEquals(1 + text.size + 64, payload.size)
        assertArrayEquals(text, payload.copyOfRange(1, 1 + text.size))
        assertArrayEquals(sig, payload.copyOfRange(payload.size - 64, payload.size))
    }

    @Test
    fun `with peer key - payload is passed through encrypt`() = runBlocking {
        val encryptedMarker = ByteArray(4) { 0xFF.toByte() }
        crypto.encryptTransform = { encryptedMarker }
        crypto.peerKeys[receiver] = ByteArray(1)
        useCase.execute(makeMessage())
        assertArrayEquals(encryptedMarker, queue.enqueued[0].payloadChunk)
    }

    @Test
    fun `with mimeType - raw payload uses attachment encoding with 0xFE marker`() = runBlocking {
        crypto.hasOwnKey = false
        val data = byteArrayOf(0x01, 0x02, 0x03)
        useCase.execute(makeMessage(payload = data, mimeType = "image/png", fileName = "photo.png"))
        val payload = queue.enqueued[0].payloadChunk
        // [0x00 flag][0xFE attachment marker]...
        assertEquals(0x00.toByte(), payload[0])
        assertEquals(SendMessageUseCase.ATTACHMENT_MARKER, payload[1])
    }

    @Test
    fun `packet sender and receiver match message`() = runBlocking {
        val msg = makeMessage()
        useCase.execute(msg)
        val packet = queue.enqueued[0]
        assertEquals(sender, packet.senderNodeId)
        assertEquals(receiver, packet.receiverNodeId)
    }
}
