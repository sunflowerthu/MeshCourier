package com.sunflowerthu.meshcourier.domain.usecase

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import javax.inject.Inject

class GetOwnNodeIdUseCase @Inject constructor(
    private val nodeIdentity: NodeIdentity
) {
    fun execute(): NodeId = nodeIdentity.nodeId
}
