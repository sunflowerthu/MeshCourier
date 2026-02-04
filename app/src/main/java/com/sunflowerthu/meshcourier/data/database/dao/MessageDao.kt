package com.sunflowerthu.meshcourier.data.database.dao

import Message
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao{
    @Query("SELECT * FROM messages")
    fun observeAll(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE status = 'PENDING'")
    fun getPending(): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message)

    @Update
    suspend fun update(message: Message)
}