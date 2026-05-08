package com.sunflowerthu.meshcourier.domain.usecase

import com.sunflowerthu.meshcourier.domain.mesh.PacketQueue
import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.Packet
import com.sunflowerthu.meshcourier.domain.models.PacketType
import com.sunflowerthu.meshcourier.domain.repository.MessageRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendMessageUseCase @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val packetQueue: PacketQueue,
    private val messageRepository: MessageRepository
) {

    suspend fun execute(message: Message) {
        messageRepository.save(message)

        val plaintext = if (message.mimeType != null) {
            encodeAttachment(message.payload, message.mimeType, message.fileName ?: "file")
        } else {
            message.payload
        }
        val signedData = withContext(Dispatchers.Default) {
            if (cryptoManager.hasOwnKeyPair()) {
                runCatching {
                    val sig = cryptoManager.sign(plaintext)
                    byteArrayOf(0x01) + plaintext + sig
                }.getOrElse { byteArrayOf(0x00) + plaintext }
            } else {
                byteArrayOf(0x00) + plaintext
            }
        }

        val payload = withContext(Dispatchers.Default) {
            if (cryptoManager.hasPeerKey(message.receiverNodeId)) {
                runCatching { cryptoManager.encrypt(signedData, message.receiverNodeId) }
                    .getOrDefault(signedData)
            } else {
                signedData
            }
        }

        val packet = Packet(
            packetId = UUID.randomUUID(),
            messageId = message.messageId,
            senderNodeId = message.senderNodeId,
            receiverNodeId = message.receiverNodeId,
            ttl = message.ttl,
            hopCount = 0,
            packetType = PacketType.MESSAGE,
            payloadChunk = payload
        )

        packetQueue.enqueue(packet)
    }

    private fun encodeAttachment(data: ByteArray, mimeType: String, fileName: String): ByteArray {
        val mimeBytes = mimeType.toByteArray(Charsets.UTF_8)
        val nameBytes = fileName.toByteArray(Charsets.UTF_8)
        val buf = ByteArray(5 + mimeBytes.size + nameBytes.size + data.size)
        buf[0] = ATTACHMENT_MARKER
        buf[1] = (mimeBytes.size shr 8).toByte()
        buf[2] = (mimeBytes.size and 0xFF).toByte()
        buf[3] = (nameBytes.size shr 8).toByte()
        buf[4] = (nameBytes.size and 0xFF).toByte()
        var pos = 5
        mimeBytes.copyInto(buf, pos); pos += mimeBytes.size
        nameBytes.copyInto(buf, pos); pos += nameBytes.size
        data.copyInto(buf, pos)
        return buf
    }

    companion object {
        const val ATTACHMENT_MARKER = 0xFE.toByte()
    }
}
