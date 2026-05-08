package com.sunflowerthu.meshcourier.data.repository

import com.sunflowerthu.meshcourier.data.database.dao.PeerKeyDao
import com.sunflowerthu.meshcourier.data.database.entities.PeerKeyEntity
import com.sunflowerthu.meshcourier.domain.repository.PeerKeyRepository
import com.sunflowerthu.meshcourier.domain.models.NodeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeerKeyRepositoryImpl @Inject constructor(private val dao: PeerKeyDao) : PeerKeyRepository {

    override suspend fun save(nodeId: NodeId, encodedPublicKey: ByteArray) {
        dao.insert(PeerKeyEntity(nodeId = nodeId.value, encodedPublicKey = encodedPublicKey))
    }

    override suspend fun loadAll(): Map<NodeId, ByteArray> {
        return dao.getAll().associate { NodeId(it.nodeId) to it.encodedPublicKey }
    }

    override suspend fun delete(nodeId: NodeId) {
        dao.delete(nodeId.value)
    }

    override fun observeKnownNodeIds(): Flow<Set<NodeId>> =
        dao.observeNodeIds().map { list -> list.map { NodeId(it) }.toSet() }
}