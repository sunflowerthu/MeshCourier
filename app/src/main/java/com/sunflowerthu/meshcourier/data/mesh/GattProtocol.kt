package com.sunflowerthu.meshcourier.data.mesh

import java.util.UUID

object GattProtocol {

    val SERVICE_UUID: UUID =
        UUID.fromString("000018FE-0000-1000-8000-00805F9B34FB")

    val PACKET_CHAR_UUID: UUID =
        UUID.fromString("00002AFE-0000-1000-8000-00805F9B34FB")

    const val DEFAULT_MTU = 247
    const val HEADER_SIZE = 2
    const val MAX_CHUNK_SIZE = DEFAULT_MTU - HEADER_SIZE
}