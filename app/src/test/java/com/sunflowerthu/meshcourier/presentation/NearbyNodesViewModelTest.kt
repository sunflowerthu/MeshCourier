package com.sunflowerthu.meshcourier.presentation

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.usecase.SendKeyExchangeUseCase
import com.sunflowerthu.meshcourier.mocks.MockContactRepository
import com.sunflowerthu.meshcourier.mocks.MockCryptoManager
import com.sunflowerthu.meshcourier.mocks.MockNearbyNodesRepository
import com.sunflowerthu.meshcourier.mocks.MockPacketQueue
import com.sunflowerthu.meshcourier.mocks.MockPeerKeyRepository
import com.sunflowerthu.meshcourier.presentation.screens.nearby.MeshStats
import com.sunflowerthu.meshcourier.presentation.screens.nearby.NearbyNodesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyNodesViewModelTest {

    private val myNode = NodeId("my-node")
    private val peerNode = NodeId("peer-node")

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var nearbyRepo: MockNearbyNodesRepository
    private lateinit var contactRepo: MockContactRepository
    private lateinit var peerKeyRepo: MockPeerKeyRepository
    private lateinit var crypto: MockCryptoManager
    private lateinit var queue: MockPacketQueue
    private lateinit var viewModel: NearbyNodesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        nearbyRepo = MockNearbyNodesRepository()
        contactRepo = MockContactRepository()
        peerKeyRepo = MockPeerKeyRepository()
        crypto = MockCryptoManager()
        queue = MockPacketQueue()
        viewModel = NearbyNodesViewModel(
            nearbyNodesRepository = nearbyRepo,
            contactRepository = contactRepo,
            peerKeyRepository = peerKeyRepo,
            sendKeyExchangeUseCase = SendKeyExchangeUseCase(crypto, queue, NodeIdentity(myNode))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial stats are all zero`() = runTest(testDispatcher) {
        val stats = viewModel.stats.first()
        assertEquals(MeshStats(0, 0, 0), stats)
    }

    @Test
    fun `nodeItems is empty when no nearby nodes`() = runTest(testDispatcher) {
        val items = viewModel.nodeItems.first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun `stats activeNodes reflects nearby nodes count`() = runTest(testDispatcher) {
        nearbyRepo.addNode(peerNode)
        val stats = viewModel.stats.first()
        assertEquals(1, stats.activeNodes)
    }

    @Test
    fun `stats packetsSent increments after incrementSent`() = runTest(testDispatcher) {
        nearbyRepo.incrementSent()
        nearbyRepo.incrementSent()
        val stats = viewModel.stats.first()
        assertEquals(2, stats.packetsSent)
    }

    @Test
    fun `stats packetsRelayed increments after incrementRelayed`() = runTest(testDispatcher) {
        nearbyRepo.incrementRelayed()
        val stats = viewModel.stats.first()
        assertEquals(1, stats.packetsRelayed)
    }

    @Test
    fun `nodeItems hasKey is false when peer key not present`() = runTest(testDispatcher) {
        nearbyRepo.addNode(peerNode)
        val items = viewModel.nodeItems.first()
        assertEquals(1, items.size)
        assertFalse(items[0].hasKey)
    }

    @Test
    fun `nodeItems shows contactName from contact repository`() = runTest(testDispatcher) {
        nearbyRepo.addNode(peerNode)
        contactRepo.setDisplayName(peerNode, "Alice")
        val items = viewModel.nodeItems.first()
        assertEquals("Alice", items[0].contactName)
    }
}
