package com.sunflowerthu.meshcourier.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val nodeId: String,
    val displayName: String
)
