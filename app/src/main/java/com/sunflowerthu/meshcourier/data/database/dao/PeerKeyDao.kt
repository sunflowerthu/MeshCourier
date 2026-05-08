package com.sunflowerthu.meshcourier.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunflowerthu.meshcourier.data.database.entities.PeerKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PeerKeyEntity)

    @Query("SELECT * FROM peer_keys")
    suspend fun getAll(): List<PeerKeyEntity>

    @Query("SELECT nodeId FROM peer_keys")
    fun observeNodeIds(): Flow<List<String>>

    @Query("SELECT * FROM peer_keys WHERE nodeId = :nodeId")
    suspend fun getByNodeId(nodeId: String): PeerKeyEntity?

    @Query("DELETE FROM peer_keys WHERE nodeId = :nodeId")
    suspend fun delete(nodeId: String)
}
