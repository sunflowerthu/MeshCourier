package com.sunflowerthu.meshcourier.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sunflowerthu.meshcourier.data.database.dao.ContactDao
import com.sunflowerthu.meshcourier.data.database.dao.MessageDao
import com.sunflowerthu.meshcourier.data.database.dao.PacketDao
import com.sunflowerthu.meshcourier.data.database.dao.PeerKeyDao
import com.sunflowerthu.meshcourier.data.database.entities.ContactEntity
import com.sunflowerthu.meshcourier.data.database.entities.MessageEntity
import com.sunflowerthu.meshcourier.data.database.entities.PacketEntity
import com.sunflowerthu.meshcourier.data.database.entities.PeerKeyEntity

@Database(
    entities = [MessageEntity::class, PeerKeyEntity::class, PacketEntity::class, ContactEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun peerKeyDao(): PeerKeyDao
    abstract fun packetDao(): PacketDao
    abstract fun contactDao(): ContactDao
}