package com.sunflowerthu.meshcourier.domain.models

/**
 * Хранит NodeId этого устройства. Предоставляется Hilt как синглтон.
 * Нужен как обёртка над value class NodeId, т.к. value class нельзя использовать
 * как lateinit var при field injection.
 */
data class NodeIdentity(val nodeId: NodeId)
