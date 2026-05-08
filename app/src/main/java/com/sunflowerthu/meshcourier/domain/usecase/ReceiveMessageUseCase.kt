package com.sunflowerthu.meshcourier.domain.usecase

import android.util.Log
import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import com.sunflowerthu.meshcourier.domain.repository.NearbyNodesRepository
import com.sunflowerthu.meshcourier.domain.mesh.PacketQueue
import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.MessageStatus
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.models.Packet
import com.sunflowerthu.meshcourier.domain.models.PacketType
import com.sunflowerthu.meshcourier.domain.repository.MessageRepository
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class ReceiveMessageUseCase @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val packetQueue: PacketQueue,
    private val messageRepository: MessageRepository,
    private val nearbyNodesRepository: NearbyNodesRepository,
    private val nodeIdentity: NodeIdentity
) {
    private val keyExchangeRespondedTo: MutableSet<NodeId> =
        Collections.newSetFromMap(ConcurrentHashMap())

    suspend fun execute(packet: Packet) {
        when (packet.packetType) {
            PacketType.MESSAGE -> handleMessage(packet)
            PacketType.KEY_EXCHANGE -> handleKeyExchange(packet)
            PacketType.ACK -> handleAck(packet)
            PacketType.HELLO -> handleHello(packet)
        }
    }

    private suspend fun handleMessage(packet: Packet) {
        val incoming = Message(
            messageId = packet.messageId,
            senderNodeId = packet.senderNodeId,
            receiverNodeId = packet.receiverNodeId,
            payload = packet.payloadChunk,
            ttl = packet.ttl,
            hopCount = packet.hopCount,
            status = MessageStatus.PROCESSING
        )
        messageRepository.save(incoming)

        val decryptedData = if (cryptoManager.hasPeerKey(packet.senderNodeId)) {
            runCatching {
                cryptoManager.decrypt(packet.payloadChunk, packet.senderNodeId)
            }.getOrDefault(packet.payloadChunk)
        } else {
            packet.payloadChunk
        }

        val finalPayload = parseAndVerify(decryptedData, packet.senderNodeId)
        val (cleanPayload, mimeType, fileName) = decodePayload(finalPayload)
        messageRepository.update(
            incoming.copy(
                payload = cleanPayload,
                mimeType = mimeType,
                fileName = fileName,
                status = MessageStatus.DELIVERED
            )
        )
        sendAck(packet)
    }

    private fun parseAndVerify(data: ByteArray, senderId: NodeId): ByteArray {
        if (data.isEmpty()) return data
        return when (data[0]) {
            0x01.toByte() -> {
                val sigSize = 64
                if (data.size <= 1 + sigSize) return data.copyOfRange(1, data.size)
                val payload = data.copyOfRange(1, data.size - sigSize)
                val signature = data.copyOfRange(data.size - sigSize, data.size)
                if (cryptoManager.hasPeerKey(senderId)) {
                    runCatching { cryptoManager.verify(payload, signature, senderId) }
                        .onSuccess { valid -> if (!valid) Log.w(TAG, "Подпись не прошла проверку от $senderId") }
                        .onFailure { Log.w(TAG, "Ошибка проверки подписи от $senderId", it) }
                }
                payload
            }
            0x00.toByte() -> if (data.size > 1) data.copyOfRange(1, data.size) else ByteArray(0)
            else -> data
        }
    }

    private suspend fun handleKeyExchange(packet: Packet) {
        runCatching {
            cryptoManager.importPeerPublicKey(packet.senderNodeId, packet.payloadChunk)
        }
        if (!cryptoManager.hasOwnKeyPair()) return
        if (keyExchangeRespondedTo.add(packet.senderNodeId)) {
            sendOwnKeyExchange(packet.senderNodeId)
        }
    }

    private fun sendOwnKeyExchange(recipientId: NodeId) {
        runCatching {
            val keyPacket = Packet(
                packetId = UUID.randomUUID(),
                messageId = UUID.randomUUID(),
                senderNodeId = nodeIdentity.nodeId,
                receiverNodeId = recipientId,
                ttl = DEFAULT_ACK_TTL,
                hopCount = 0,
                packetType = PacketType.KEY_EXCHANGE,
                payloadChunk = cryptoManager.getPublicKeyEncoded()
            )
            packetQueue.enqueue(keyPacket)
        }
    }

    private suspend fun handleAck(packet: Packet) {
        runCatching {
            messageRepository.getById(packet.messageId)?.let { original ->
                messageRepository.update(original.copy(status = MessageStatus.DELIVERED))
            }
        }
    }

    private fun handleHello(packet: Packet) {
        val senderId = packet.senderNodeId
        if (senderId == nodeIdentity.nodeId) return

        val isNew = nearbyNodesRepository.addNode(senderId)
        if (isNew) {
            packetQueue.enqueue(buildHello(nodeIdentity.nodeId, senderId))
        }
    }

    private data class DecodedPayload(val data: ByteArray, val mimeType: String?, val fileName: String?)

    private fun decodePayload(payload: ByteArray): DecodedPayload {
        if (payload.size >= 5 && payload[0] == SendMessageUseCase.ATTACHMENT_MARKER) {
            runCatching {
                val mimeLen = ((payload[1].toInt() and 0xFF) shl 8) or (payload[2].toInt() and 0xFF)
                val nameLen = ((payload[3].toInt() and 0xFF) shl 8) or (payload[4].toInt() and 0xFF)
                var pos = 5
                val mimeType = String(payload, pos, mimeLen, Charsets.UTF_8); pos += mimeLen
                val fileName = String(payload, pos, nameLen, Charsets.UTF_8); pos += nameLen
                val data = payload.copyOfRange(pos, payload.size)
                return DecodedPayload(data, mimeType, fileName)
            }
        }
        return DecodedPayload(payload, null, null)
    }

    private fun sendAck(original: Packet) {
        val ack = Packet(
            packetId = UUID.randomUUID(),
            messageId = original.messageId,
            senderNodeId = original.receiverNodeId,
            receiverNodeId = original.senderNodeId,
            ttl = DEFAULT_ACK_TTL,
            hopCount = 0,
            packetType = PacketType.ACK,
            payloadChunk = ByteArray(1)
        )
        packetQueue.enqueue(ack)
    }

    companion object {
        private const val TAG = "ReceiveMessageUseCase"
        private const val DEFAULT_ACK_TTL = 64

        fun buildHello(from: NodeId, to: NodeId): Packet = Packet(
            packetId = UUID.randomUUID(),
            messageId = UUID.randomUUID(),
            senderNodeId = from,
            receiverNodeId = to,
            ttl = 1,
            hopCount = 0,
            packetType = PacketType.HELLO,
            payloadChunk = ByteArray(1)
        )
    }
}
