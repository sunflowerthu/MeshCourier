package com.sunflowerthu.meshcourier.mocks

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.repository.NearbyNodesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockNearbyNodesRepository : NearbyNodesRepository {
    private val _nodes = MutableStateFlow<List<NodeId>>(emptyList())
    override val nodes: StateFlow<List<NodeId>> = _nodes
    override val lastSeenTimestamps = MutableStateFlow<Map<NodeId, Long>>(emptyMap())
    override val packetsSent = MutableStateFlow(0)
    override val packetsRelayed = MutableStateFlow(0)
    private val knownNodes = mutableSetOf<NodeId>()

    override fun addNode(nodeId: NodeId): Boolean {
        return if (knownNodes.add(nodeId)) {
            _nodes.value = knownNodes.toList()
            true
        } else false
    }

    override fun incrementSent() { packetsSent.value++ }
    override fun incrementRelayed() { packetsRelayed.value++ }
}
