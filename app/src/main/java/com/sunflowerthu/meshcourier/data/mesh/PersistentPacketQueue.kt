package com.sunflowerthu.meshcourier.data.mesh

import com.sunflowerthu.meshcourier.data.database.dao.PacketDao
import com.sunflowerthu.meshcourier.data.database.entities.PacketEntity
import com.sunflowerthu.meshcourier.domain.mesh.PacketQueue
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.Packet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PacketQueue с персистентностью через Room.
 * enqueue() — сохраняет в БД асинхронно.
 * drainAll() — удаляет из БД после выгрузки.
 * restore() — при старте сервиса загружает пакеты обратно в память.
 */
@Singleton
class PersistentPacketQueue @Inject constructor(
    private val dao: PacketDao
) : PacketQueue() {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun restore() {
        val now = System.currentTimeMillis()
        dao.getAll().forEach { entity ->
            if (now - entity.enqueuedAt > PacketQueue.TTL_MS) {
                ioScope.launch { dao.delete(entity.packetId) }
            } else {
                enqueueAt(entity.toDomain(), entity.enqueuedAt)
            }
        }
    }

    override fun enqueue(packet: Packet) {
        super.enqueue(packet)
        ioScope.launch { dao.insert(packet.toEntity()) }
    }

    override fun drainAll(): List<Packet> {
        val packets = super.drainAll()
        if (packets.isNotEmpty()) {
            val ids = packets.map { it.packetId.toString() }
            ioScope.launch { ids.forEach { dao.delete(it) } }
        }
        return packets
    }

    override fun dropExpired(): List<Packet> {
        val expired = super.dropExpired()
        if (expired.isNotEmpty()) {
            val ids = expired.map { it.packetId.toString() }
            ioScope.launch { ids.forEach { dao.delete(it) } }
        }
        return expired
    }

    private fun Packet.toEntity() = PacketEntity(
        packetId = packetId.toString(),
        messageId = messageId.toString(),
        senderNodeId = senderNodeId.value,
        receiverNodeId = receiverNodeId.value,
        ttl = ttl,
        hopCount = hopCount,
        packetType = packetType,
        payloadChunk = payloadChunk
    )

    private fun PacketEntity.toDomain() = Packet(
        packetId = UUID.fromString(packetId),
        messageId = UUID.fromString(messageId),
        senderNodeId = NodeId(senderNodeId),
        receiverNodeId = NodeId(receiverNodeId),
        ttl = ttl,
        hopCount = hopCount,
        packetType = packetType,
        payloadChunk = payloadChunk
    )
}
