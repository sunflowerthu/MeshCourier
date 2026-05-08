package com.sunflowerthu.meshcourier.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peer_keys")
data class PeerKeyEntity(
    @PrimaryKey
    val nodeId: String,
    val encodedPublicKey: ByteArray,
    val importedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PeerKeyEntity) return false
        return nodeId == other.nodeId && encodedPublicKey.contentEquals(other.encodedPublicKey)
    }

    override fun hashCode(): Int {
        var result = nodeId.hashCode()
        result = 31 * result + encodedPublicKey.contentHashCode()
        return result
    }
}
