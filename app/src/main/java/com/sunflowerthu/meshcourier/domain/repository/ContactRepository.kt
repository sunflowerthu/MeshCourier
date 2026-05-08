package com.sunflowerthu.meshcourier.domain.repository

import com.sunflowerthu.meshcourier.domain.models.NodeId
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    suspend fun setDisplayName(nodeId: NodeId, name: String)
    suspend fun delete(nodeId: NodeId)
    fun observeDisplayName(nodeId: NodeId): Flow<String?>
    fun observeAll(): Flow<Map<NodeId, String>>
}
