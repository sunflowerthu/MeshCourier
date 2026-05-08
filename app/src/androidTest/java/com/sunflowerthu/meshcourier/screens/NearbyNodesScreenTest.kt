package com.sunflowerthu.meshcourier.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.usecase.SendKeyExchangeUseCase
import com.sunflowerthu.meshcourier.mocks.MockContactRepository
import com.sunflowerthu.meshcourier.mocks.MockCryptoManager
import com.sunflowerthu.meshcourier.mocks.MockNearbyNodesRepository
import com.sunflowerthu.meshcourier.mocks.MockPacketQueue
import com.sunflowerthu.meshcourier.mocks.MockPeerKeyRepository
import com.sunflowerthu.meshcourier.presentation.screens.nearby.NearbyNodesScreen
import com.sunflowerthu.meshcourier.presentation.screens.nearby.NearbyNodesViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NearbyNodesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val myNode = NodeId("test-node")

    private fun buildViewModel(
        nearbyRepo: MockNearbyNodesRepository = MockNearbyNodesRepository()
    ) = NearbyNodesViewModel(
        nearbyNodesRepository = nearbyRepo,
        contactRepository = MockContactRepository(),
        peerKeyRepository = MockPeerKeyRepository(),
        sendKeyExchangeUseCase = SendKeyExchangeUseCase(
            MockCryptoManager(), MockPacketQueue(), NodeIdentity(myNode)
        )
    )

    @Test
    fun shows_empty_state_when_no_nearby_nodes() {
        composeRule.setContent {
            NearbyNodesScreen(onOpenChat = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText("Нет устройств рядом").assertIsDisplayed()
    }

    @Test
    fun shows_nearby_title_in_top_bar() {
        composeRule.setContent {
            NearbyNodesScreen(onOpenChat = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText("Рядом").assertIsDisplayed()
    }

    @Test
    fun shows_stats_card_labels() {
        composeRule.setContent {
            NearbyNodesScreen(onOpenChat = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText("Узлов").assertIsDisplayed()
        composeRule.onNodeWithText("Отправлено").assertIsDisplayed()
        composeRule.onNodeWithText("Ретранслировано").assertIsDisplayed()
    }

    @Test
    fun shows_nearby_node_with_exchange_keys_button_when_no_key() {
        val nearbyRepo = MockNearbyNodesRepository()
        nearbyRepo.addNode(NodeId("peer-001"))
        composeRule.setContent {
            NearbyNodesScreen(onOpenChat = {}, viewModel = buildViewModel(nearbyRepo))
        }
        composeRule.onNodeWithText("Обменяться ключами").assertIsDisplayed()
    }
}
