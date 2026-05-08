package com.sunflowerthu.meshcourier.domain.protocol

object PacketFormat {

    const val VERSION: Byte = 1

    const val BYTE_SIZE = 1
    const val SHORT_SIZE = 2
    const val INT_SIZE = 4
    const val UUID_SIZE = 16

    const val MAX_NODE_ID_LENGTH = 255
    const val MAX_PAYLOAD_SIZE = 32_000

    const val MIN_HEADER_SIZE =
        BYTE_SIZE + // version
                BYTE_SIZE + // packetType
                BYTE_SIZE + // ttl
                BYTE_SIZE + // hopCount
                UUID_SIZE + // packetId
                UUID_SIZE + // messageId
                BYTE_SIZE + // fromNodeId length
                BYTE_SIZE + // toNodeId length
                SHORT_SIZE  // payload length
}