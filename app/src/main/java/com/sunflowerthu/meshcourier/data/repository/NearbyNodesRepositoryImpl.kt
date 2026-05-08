package com.sunflowerthu.meshcourier.data.repository

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.repository.NearbyNodesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyNodesRepositoryImpl @Inject constructor() : NearbyNodesRepository {

    private val nodeSet = Collections.newSetFromMap(ConcurrentHashMap<NodeId, Boolean>())
    private val _nodes = MutableStateFlow<List<NodeId>>(emptyList())
    override val nodes: StateFlow<List<NodeId>> = _nodes.asStateFlow()

    private val _lastSeenTimestamps = MutableStateFlow<Map<NodeId, Long>>(emptyMap())
    override val lastSeenTimestamps: StateFlow<Map<NodeId, Long>> = _lastSeenTimestamps.asStateFlow()

    private val _packetsSent = MutableStateFlow(0)
    override val packetsSent: StateFlow<Int> = _packetsSent.asStateFlow()

    private val _packetsRelayed = MutableStateFlow(0)
    override val packetsRelayed: StateFlow<Int> = _packetsRelayed.asStateFlow()

    override fun addNode(nodeId: NodeId): Boolean {
        val added = nodeSet.add(nodeId)
        if (added) _nodes.value = nodeSet.toList()
        _lastSeenTimestamps.value = _lastSeenTimestamps.value + (nodeId to System.currentTimeMillis())
        return added
    }

    override fun incrementSent() { _packetsSent.value++ }
    override fun incrementRelayed() { _packetsRelayed.value++ }
}
