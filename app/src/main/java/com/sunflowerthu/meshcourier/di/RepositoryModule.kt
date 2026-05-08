package com.sunflowerthu.meshcourier.di

import com.sunflowerthu.meshcourier.data.repository.ContactRepositoryImpl
import com.sunflowerthu.meshcourier.data.repository.PeerKeyRepositoryImpl
import com.sunflowerthu.meshcourier.data.repository.NearbyNodesRepositoryImpl
import com.sunflowerthu.meshcourier.data.repository.MessageRepositoryImpl
import com.sunflowerthu.meshcourier.domain.repository.ContactRepository
import com.sunflowerthu.meshcourier.domain.repository.PeerKeyRepository
import com.sunflowerthu.meshcourier.domain.repository.NearbyNodesRepository
import com.sunflowerthu.meshcourier.domain.repository.MessageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindPeerKeyRepository(impl: PeerKeyRepositoryImpl): PeerKeyRepository

    @Binds
    @Singleton
    abstract fun bindNearbyNodesRepository(impl: NearbyNodesRepositoryImpl): NearbyNodesRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository
}
