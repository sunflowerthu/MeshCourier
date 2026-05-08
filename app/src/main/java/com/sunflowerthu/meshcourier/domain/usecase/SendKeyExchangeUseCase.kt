package com.sunflowerthu.meshcourier.domain.usecase

import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import com.sunflowerthu.meshcourier.domain.mesh.PacketQueue
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.models.Packet
import com.sunflowerthu.meshcourier.domain.models.PacketType
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendKeyExchangeUseCase @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val packetQueue: PacketQueue,
    private val nodeIdentity: NodeIdentity
) {
    suspend fun execute(recipientNodeId: NodeId) {
        val publicKeyBytes = withContext(Dispatchers.Default) {
            if (!cryptoManager.hasOwnKeyPair()) {
                cryptoManager.generateOwnKeyPair()
            }
            cryptoManager.getPublicKeyEncoded()
        }

        val packet = Packet(
            packetId = UUID.randomUUID(),
            messageId = UUID.randomUUID(),
            senderNodeId = nodeIdentity.nodeId,
            receiverNodeId = recipientNodeId,
            ttl = 64,
            hopCount = 0,
            packetType = PacketType.KEY_EXCHANGE,
            payloadChunk = publicKeyBytes
        )
        packetQueue.enqueue(packet)
    }
}
