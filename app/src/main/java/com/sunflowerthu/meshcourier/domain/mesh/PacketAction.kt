package com.sunflowerthu.meshcourier.domain.mesh

import com.sunflowerthu.meshcourier.domain.models.Packet

sealed class PacketAction {

    /**
     * Пакет предназначен текущему узлу (в т.ч. ACK).
     */
    data class Deliver(val packet: Packet, val fromAddress: String? = null) : PacketAction()

    /** Пакет нужно ретранслировать дальше */
    data class Forward(val packet: Packet, val fromAddress: String? = null) : PacketAction()

    /** Пакет нужно отбросить */
    object Drop : PacketAction()
}