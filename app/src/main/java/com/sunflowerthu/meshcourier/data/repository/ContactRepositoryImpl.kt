package com.sunflowerthu.meshcourier.data.repository

import com.sunflowerthu.meshcourier.data.database.dao.ContactDao
import com.sunflowerthu.meshcourier.data.database.entities.ContactEntity
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(private val dao: ContactDao) : ContactRepository {

    override suspend fun setDisplayName(nodeId: NodeId, name: String) {
        dao.insertOrUpdate(ContactEntity(nodeId.value, name))
    }

    override suspend fun delete(nodeId: NodeId) = dao.delete(nodeId.value)

    override fun observeDisplayName(nodeId: NodeId): Flow<String?> =
        dao.observeByNodeId(nodeId.value).map { it?.displayName }

    override fun observeAll(): Flow<Map<NodeId, String>> =
        dao.observeAll().map { list -> list.associate { NodeId(it.nodeId) to it.displayName } }
}
