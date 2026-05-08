package com.sunflowerthu.meshcourier.di

import android.content.Context
import com.sunflowerthu.meshcourier.data.mesh.PersistentPacketQueue
import com.sunflowerthu.meshcourier.domain.mesh.PacketQueue
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import javax.inject.Singleton
import androidx.core.content.edit

@Module
@InstallIn(SingletonComponent::class)
abstract class MeshModule {

    @Binds
    @Singleton
    abstract fun bindPacketQueue(impl: PersistentPacketQueue): PacketQueue

    companion object {
        private const val PREFS_NAME = "mesh_prefs"
        private const val KEY_NODE_ID = "node_id"

        @Provides
        @Singleton
        fun provideNodeIdentity(@ApplicationContext context: Context): NodeIdentity {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getString(KEY_NODE_ID, null)
            val id = stored ?: UUID.randomUUID().toString().also {
                prefs.edit { putString(KEY_NODE_ID, it) }
            }
            return NodeIdentity(NodeId(id))
        }
    }
}
