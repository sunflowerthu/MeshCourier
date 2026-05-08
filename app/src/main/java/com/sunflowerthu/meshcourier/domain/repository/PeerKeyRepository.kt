package com.sunflowerthu.meshcourier.domain.repository

import com.sunflowerthu.meshcourier.domain.models.NodeId
import kotlinx.coroutines.flow.Flow

interface PeerKeyRepository {
    suspend fun save(nodeId: NodeId, encodedPublicKey: ByteArray)
    suspend fun loadAll(): Map<NodeId, ByteArray>
    suspend fun delete(nodeId: NodeId)
    fun observeKnownNodeIds(): Flow<Set<NodeId>>
}