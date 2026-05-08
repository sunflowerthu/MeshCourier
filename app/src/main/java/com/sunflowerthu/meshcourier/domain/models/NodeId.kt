package com.sunflowerthu.meshcourier.domain.models

@JvmInline
value class NodeId(val value: String) {
    init {
        require(value.isNotBlank())
    }

    companion object {
        val BROADCAST = NodeId("__broadcast__")
    }
}