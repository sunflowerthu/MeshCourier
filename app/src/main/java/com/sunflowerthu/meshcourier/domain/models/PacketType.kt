package com.sunflowerthu.meshcourier.domain.models

enum class PacketType(val priority: Int) {
    ACK(0),
    KEY_EXCHANGE(1),
    HELLO(2),
    MESSAGE(3),
}