package com.sunflowerthu.meshcourier.domain.mesh

import com.sunflowerthu.meshcourier.domain.models.Packet
import java.util.concurrent.PriorityBlockingQueue

/**
 * Потокобезопасная очередь пакетов с приоритетами.
 * Порядок отправки: ACK, KEY_EXCHANGE, HELLO, MESSAGE.
 * Пакеты накапливаются здесь пока подходящий узел не окажется в зоне досягаемости (DTN-буфер).
 *
 * Каждый пакет помечается временем постановки в очередь. Пакеты старше [TTL_MS]
 * выдаются методом [dropExpired] — вызывающий обязан их корректно похоронить
 * (например, пометить соответствующее сообщение как TTL_EXPIRED).
 */
open class PacketQueue {

    private data class Stored(val packet: Packet, val enqueuedAt: Long)

    private val queue = PriorityBlockingQueue<Stored>(16, compareBy { it.packet.packetType.priority })

    open fun enqueue(packet: Packet) {
        enqueueAt(packet, System.currentTimeMillis())
    }

    protected fun enqueueAt(packet: Packet, enqueuedAt: Long) {
        queue.add(Stored(packet, enqueuedAt))
    }

    open fun drainAll(): List<Packet> {
        val result = mutableListOf<Packet>()
        while (true) result.add((queue.poll() ?: break).packet)
        return result
    }

    open fun dropExpired(): List<Packet> {
        val now = System.currentTimeMillis()
        val expired = mutableListOf<Packet>()
        queue.removeIf { stored ->
            val isOld = now - stored.enqueuedAt > TTL_MS
            if (isOld) expired.add(stored.packet)
            isOld
        }
        return expired
    }

    fun isEmpty(): Boolean = queue.isEmpty()

    fun size(): Int = queue.size

    companion object {
        const val TTL_MS = 24 * 60 * 60 * 1000L
    }
}
