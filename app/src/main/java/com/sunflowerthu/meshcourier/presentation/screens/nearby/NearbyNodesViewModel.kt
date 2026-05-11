package com.sunflowerthu.meshcourier.presentation.screens.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import com.sunflowerthu.meshcourier.domain.repository.ContactRepository
import com.sunflowerthu.meshcourier.domain.repository.NearbyNodesRepository
import com.sunflowerthu.meshcourier.domain.repository.PeerKeyRepository
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.usecase.SendKeyExchangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NearbyNodeItem(
    val nodeId: NodeId,
    val hasKey: Boolean,
    val contactName: String?,
    val lastSeenAt: Long?,
)

data class MeshStats(
    val activeNodes: Int,
    val packetsSent: Int,
    val packetsRelayed: Int,
)

@HiltViewModel
class NearbyNodesViewModel @Inject constructor(
    private val nearbyNodesRepository: NearbyNodesRepository,
    contactRepository: ContactRepository,
    peerKeyRepository: PeerKeyRepository,
    private val sendKeyExchangeUseCase: SendKeyExchangeUseCase,
    private val cryptoManager: CryptoManager,
) : ViewModel() {

    val nodeItems = combine(
        nearbyNodesRepository.nodes,
        nearbyNodesRepository.lastSeenTimestamps,
        contactRepository.observeAll(),
        peerKeyRepository.observeKnownNodeIds()
    ) { nodes, timestamps, contactNames, knownKeys ->
        nodes.map { nodeId ->
            NearbyNodeItem(
                nodeId = nodeId,
                hasKey = nodeId in knownKeys,
                contactName = contactNames[nodeId],
                lastSeenAt = timestamps[nodeId]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats = combine(
        nearbyNodesRepository.nodes,
        nearbyNodesRepository.packetsSent,
        nearbyNodesRepository.packetsRelayed
    ) { nodes, sent, relayed ->
        MeshStats(activeNodes = nodes.size, packetsSent = sent, packetsRelayed = relayed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeshStats(0, 0, 0))

    fun hasOwnKeyPair(): Boolean = cryptoManager.hasOwnKeyPair()

    fun sendKeyExchange(nodeId: NodeId) {
        viewModelScope.launch {
            runCatching { sendKeyExchangeUseCase.execute(nodeId) }
        }
    }
}
