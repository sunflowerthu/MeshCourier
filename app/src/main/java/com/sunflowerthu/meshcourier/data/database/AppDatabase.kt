package com.sunflowerthu.meshcourier.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sunflowerthu.meshcourier.data.database.dao.MessageDao
import com.sunflowerthu.meshcourier.data.database.entities.MessageEntity

@Database(entities = [MessageEntity::class], version = 1)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}