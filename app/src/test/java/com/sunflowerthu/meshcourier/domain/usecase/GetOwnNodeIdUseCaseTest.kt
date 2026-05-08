package com.sunflowerthu.meshcourier.domain.usecase

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import org.junit.Assert.*
import org.junit.Test

class GetOwnNodeIdUseCaseTest {

    @Test
    fun `execute returns node id from identity`() {
        val id = NodeId("my-node-id")
        val useCase = GetOwnNodeIdUseCase(NodeIdentity(id))
        assertEquals(id, useCase.execute())
    }
}
