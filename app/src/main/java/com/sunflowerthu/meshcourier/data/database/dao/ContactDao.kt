package com.sunflowerthu.meshcourier.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunflowerthu.meshcourier.data.database.entities.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ContactEntity)

    @Query("SELECT * FROM contacts WHERE nodeId = :nodeId")
    fun observeByNodeId(nodeId: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("DELETE FROM contacts WHERE nodeId = :nodeId")
    suspend fun delete(nodeId: String)
}
