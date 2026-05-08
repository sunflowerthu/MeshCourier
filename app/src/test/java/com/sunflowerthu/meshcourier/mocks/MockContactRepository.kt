package com.sunflowerthu.meshcourier.mocks

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MockContactRepository : ContactRepository {
    private val names = MutableStateFlow<Map<NodeId, String>>(emptyMap())

    override suspend fun setDisplayName(nodeId: NodeId, name: String) {
        names.value = names.value + (nodeId to name)
    }

    override suspend fun delete(nodeId: NodeId) {
        names.value = names.value - nodeId
    }

    override fun observeDisplayName(nodeId: NodeId): Flow<String?> =
        names.map { it[nodeId] }

    override fun observeAll(): Flow<Map<NodeId, String>> = names
}
