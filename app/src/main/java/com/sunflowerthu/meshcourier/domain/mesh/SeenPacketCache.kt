package com.sunflowerthu.meshcourier.domain.mesh

import java.util.UUID

interface SeenPacketCache {

    /**
     * @return true, если пакет с таким packetId уже был обработан
     */
    fun hasSeen(packetId: UUID): Boolean

    /**
     * Помечает пакет как уже обработанный
     */
    fun markSeen(packetId: UUID)
}