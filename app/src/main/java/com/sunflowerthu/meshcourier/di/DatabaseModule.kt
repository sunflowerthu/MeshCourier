package com.sunflowerthu.meshcourier.di

import android.content.Context
import androidx.room.Room
import com.sunflowerthu.meshcourier.data.database.AppDatabase
import com.sunflowerthu.meshcourier.data.database.dao.ContactDao
import com.sunflowerthu.meshcourier.data.database.dao.MessageDao
import com.sunflowerthu.meshcourier.data.database.dao.PacketDao
import com.sunflowerthu.meshcourier.data.database.dao.PeerKeyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mesh_db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun providePeerKeyDao(db: AppDatabase): PeerKeyDao = db.peerKeyDao()

    @Provides
    fun providePacketDao(db: AppDatabase): PacketDao = db.packetDao()

    @Provides
    fun provideContactDao(db: AppDatabase): ContactDao = db.contactDao()
}
