package com.sunflowerthu.meshcourier.data.database

import androidx.room.TypeConverter
import com.sunflowerthu.meshcourier.domain.models.MessageStatus
import com.sunflowerthu.meshcourier.domain.models.PacketType

class Converter {
    @TypeConverter fun fromMessageStatus(v: MessageStatus): String = v.name
    @TypeConverter fun toMessageStatus(v: String): MessageStatus = MessageStatus.valueOf(v)

    @TypeConverter fun fromPacketType(v: PacketType): String = v.name
    @TypeConverter fun toPacketType(v: String): PacketType = PacketType.valueOf(v)
}