package com.sunflowerthu.meshcourier.data.mesh

import com.sunflowerthu.meshcourier.domain.mesh.SeenPacketCache
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemorySeenPacketCache(
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val cleanupIntervalMillis: Long = CLEANUP_INTERVAL_MILLIS
) : SeenPacketCache {

    private val seenPackets = ConcurrentHashMap<UUID, Long>()
    private var lastCleanupAt = 0L

    override fun hasSeen(packetId: UUID): Boolean = seenPackets.containsKey(packetId)

    override fun markSeen(packetId: UUID) {
        seenPackets[packetId] = System.currentTimeMillis()
        cleanupIfDue()
    }

    private fun cleanupIfDue() {
        val now = System.currentTimeMillis()
        if (now - lastCleanupAt < cleanupIntervalMillis) return
        lastCleanupAt = now
        val iterator = seenPackets.entries.iterator()
        while (iterator.hasNext()) {
            if (now - iterator.next().value > ttlMillis) iterator.remove()
        }
    }

    companion object {
        private const val DEFAULT_TTL_MILLIS = 30 * 60 * 1000L
        private const val CLEANUP_INTERVAL_MILLIS = 5 * 60 * 1000L // раз в 5 минут
    }
}