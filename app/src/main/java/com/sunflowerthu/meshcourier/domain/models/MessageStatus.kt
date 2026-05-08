package com.sunflowerthu.meshcourier.domain.models

enum class MessageStatus {
    PENDING,
    IN_TRANSIT,
    DELIVERED,
    PROCESSING,
    TTL_EXPIRED,
}