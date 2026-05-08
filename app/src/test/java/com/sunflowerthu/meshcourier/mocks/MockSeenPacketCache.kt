package com.sunflowerthu.meshcourier.mocks

import com.sunflowerthu.meshcourier.domain.mesh.SeenPacketCache
import java.util.UUID

class MockSeenPacketCache : SeenPacketCache {
    private val seen = mutableSetOf<UUID>()

    override fun hasSeen(packetId: UUID) = packetId in seen
    override fun markSeen(packetId: UUID) { seen.add(packetId) }
}
