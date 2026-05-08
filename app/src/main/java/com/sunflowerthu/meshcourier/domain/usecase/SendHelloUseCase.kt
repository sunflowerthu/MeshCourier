package com.sunflowerthu.meshcourier.domain.usecase

import com.sunflowerthu.meshcourier.domain.mesh.PacketQueue
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import javax.inject.Inject

class SendHelloUseCase @Inject constructor(
    private val packetQueue: PacketQueue,
    private val nodeIdentity: NodeIdentity
) {
    fun execute() {
        packetQueue.enqueue(
            ReceiveMessageUseCase.buildHello(
                from = nodeIdentity.nodeId,
                to = NodeId.BROADCAST
            )
        )
    }
}
