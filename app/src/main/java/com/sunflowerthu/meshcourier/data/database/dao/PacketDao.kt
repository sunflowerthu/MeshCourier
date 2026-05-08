package com.sunflowerthu.meshcourier.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunflowerthu.meshcourier.data.database.entities.PacketEntity

@Dao
interface PacketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(packet: PacketEntity)

    @Query("SELECT * FROM packets ORDER BY enqueuedAt ASC")
    suspend fun getAll(): List<PacketEntity>

    @Query("DELETE FROM packets WHERE packetId = :packetId")
    suspend fun delete(packetId: String)

    @Query("DELETE FROM packets")
    suspend fun deleteAll()
}
