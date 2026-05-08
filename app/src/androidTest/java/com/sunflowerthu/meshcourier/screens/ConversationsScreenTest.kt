package com.sunflowerthu.meshcourier.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.usecase.GetOwnNodeIdUseCase
import com.sunflowerthu.meshcourier.mocks.MockContactRepository
import com.sunflowerthu.meshcourier.mocks.MockCryptoManager
import com.sunflowerthu.meshcourier.mocks.MockMessageRepository
import com.sunflowerthu.meshcourier.presentation.screens.conversations.ConversationsScreen
import com.sunflowerthu.meshcourier.presentation.screens.conversations.ConversationsViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val myNode = NodeId("test-node")

    private fun buildViewModel() = ConversationsViewModel(
        messageRepository = MockMessageRepository(),
        contactRepository = MockContactRepository(),
        getOwnNodeIdUseCase = GetOwnNodeIdUseCase(NodeIdentity(myNode)),
        cryptoManager = MockCryptoManager()
    )

    @Test
    fun shows_empty_state_when_no_conversations() {
        composeRule.setContent {
            ConversationsScreen(onOpenChat = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText("Нет сообщений").assertIsDisplayed()
    }

    @Test
    fun shows_conversations_title_in_top_bar() {
        composeRule.setContent {
            ConversationsScreen(onOpenChat = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText("Чаты").assertIsDisplayed()
    }

    @Test
    fun new_chat_dialog_opens_on_fab_click() {
        composeRule.setContent {
            ConversationsScreen(onOpenChat = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText("Новый чат").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Новый чат").performClick()
        composeRule.onNodeWithText("Новый чат").assertIsDisplayed()
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionsProvider.onNodeWithContentDescription(
    description: String
) = this.onNode(
    androidx.compose.ui.test.hasContentDescription(description)
)
