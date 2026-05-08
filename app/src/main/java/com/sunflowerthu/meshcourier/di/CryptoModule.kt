package com.sunflowerthu.meshcourier.di

import com.sunflowerthu.meshcourier.data.crypto.CryptoManagerImpl
import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {

    @Binds
    @Singleton
    abstract fun bindCryptoManager(impl: CryptoManagerImpl): CryptoManager
}
