package com.sunflowerthu.meshcourier.mocks

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.repository.PeerKeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class MockPeerKeyRepository : PeerKeyRepository {
    private val keys = MutableStateFlow<Map<NodeId, ByteArray>>(emptyMap())

    override suspend fun save(nodeId: NodeId, encodedPublicKey: ByteArray) {
        keys.value = keys.value + (nodeId to encodedPublicKey)
    }

    override suspend fun loadAll() = keys.value

    override suspend fun delete(nodeId: NodeId) {
        keys.value = keys.value - nodeId
    }

    override fun observeKnownNodeIds(): Flow<Set<NodeId>> =
        MutableStateFlow(keys.value.keys)
}
