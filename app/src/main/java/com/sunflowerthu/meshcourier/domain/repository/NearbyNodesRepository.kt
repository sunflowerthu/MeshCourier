package com.sunflowerthu.meshcourier.domain.repository

import com.sunflowerthu.meshcourier.domain.models.NodeId
import kotlinx.coroutines.flow.StateFlow

interface NearbyNodesRepository {
    val nodes: StateFlow<List<NodeId>>
    val lastSeenTimestamps: StateFlow<Map<NodeId, Long>>
    val packetsSent: StateFlow<Int>
    val packetsRelayed: StateFlow<Int>

    /** Добавляет узел и обновляет время последнего контакта. @return true если узел новый */
    fun addNode(nodeId: NodeId): Boolean
    fun incrementSent()
    fun incrementRelayed()
}