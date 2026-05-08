package com.sunflowerthu.meshcourier.domain.mesh

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeId.Companion.BROADCAST
import com.sunflowerthu.meshcourier.domain.models.Packet
import com.sunflowerthu.meshcourier.domain.models.PacketType

class PacketProcessor(
    private val myNodeId: NodeId,
    private val seenPacketCache: SeenPacketCache
) {

    /**
     * Обрабатывает входящий пакет и возвращает решение,
     * что с ним нужно сделать.
     */
    fun process(packet: Packet): PacketAction {

        // Дедупликация
        if (seenPacketCache.hasSeen(packet.packetId)) {
            return PacketAction.Drop
        }
        seenPacketCache.markSeen(packet.packetId)

        // Если пакет адресован мне или всем (broadcast)
        if (packet.receiverNodeId == myNodeId || packet.receiverNodeId == BROADCAST) {
            return when (packet.packetType) {
                PacketType.MESSAGE,
                PacketType.KEY_EXCHANGE -> PacketAction.Deliver(packet)

                PacketType.ACK -> PacketAction.Deliver(packet)

                PacketType.HELLO -> PacketAction.Deliver(packet)
            }
        }

        // Если TTL истёк — дроп
        if (packet.ttl <= 0) {
            return PacketAction.Drop
        }

        // Ретрансляция
        return PacketAction.Forward(packet.forwarded())
    }
}