package com.sunflowerthu.meshcourier.mocks

import com.sunflowerthu.meshcourier.domain.mesh.PacketQueue
import com.sunflowerthu.meshcourier.domain.models.Packet

class MockPacketQueue : PacketQueue() {
    val enqueued = mutableListOf<Packet>()

    override fun enqueue(packet: Packet) {
        enqueued.add(packet)
        super.enqueue(packet)
    }
}
