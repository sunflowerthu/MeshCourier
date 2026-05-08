package com.sunflowerthu.meshcourier.presentation

import com.sunflowerthu.meshcourier.domain.models.Message
import com.sunflowerthu.meshcourier.domain.models.MessageStatus
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.usecase.GetOwnNodeIdUseCase
import com.sunflowerthu.meshcourier.mocks.MockContactRepository
import com.sunflowerthu.meshcourier.mocks.MockCryptoManager
import com.sunflowerthu.meshcourier.mocks.MockMessageRepository
import com.sunflowerthu.meshcourier.presentation.screens.conversations.ConversationItem
import com.sunflowerthu.meshcourier.presentation.screens.conversations.ConversationsViewModel
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsViewModelTest {

    private val myNode = NodeId("my-node")
    private val peerNode = NodeId("peer-node")

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var messageRepo: MockMessageRepository
    private lateinit var contactRepo: MockContactRepository
    private lateinit var crypto: MockCryptoManager
    private lateinit var viewModel: ConversationsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        messageRepo = MockMessageRepository()
        contactRepo = MockContactRepository()
        crypto = MockCryptoManager()
        viewModel = ConversationsViewModel(
            messageRepository = messageRepo,
            contactRepository = contactRepo,
            getOwnNodeIdUseCase = GetOwnNodeIdUseCase(NodeIdentity(myNode)),
            cryptoManager = crypto
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- parseAndImportQr ---

    @Test
    fun `parseAndImportQr returns nodeId for plain nodeId string`() {
        assertEquals("peer-abc", viewModel.parseAndImportQr("peer-abc"))
    }

    @Test
    fun `parseAndImportQr trims whitespace from nodeId`() {
        assertEquals("peer-abc", viewModel.parseAndImportQr("  peer-abc  "))
    }

    @Test
    fun `parseAndImportQr returns nodeId for nodeId pipe base64 format`() {
        assertEquals("peer-abc", viewModel.parseAndImportQr("peer-abc|SGVsbG8="))
    }

    @Test
    fun `parseAndImportQr returns null for empty string`() {
        assertNull(viewModel.parseAndImportQr(""))
    }

    @Test
    fun `parseAndImportQr returns null for blank string`() {
        assertNull(viewModel.parseAndImportQr("   "))
    }

    @Test
    fun `parseAndImportQr returns null when nodeId part is blank`() {
        assertNull(viewModel.parseAndImportQr("|SGVsbG8="))
    }

    // --- conversations state ---

    @Test
    fun `conversations is empty when no messages`() = runTest(testDispatcher) {
        val result = viewModel.conversations.first()
        assertEquals(emptyList<ConversationItem>(), result)
    }

    @Test
    fun `conversations groups messages by contact`() = runTest(testDispatcher) {
        val msgId = UUID.randomUUID()
        messageRepo.save(
            Message(
                messageId = msgId,
                senderNodeId = myNode,
                receiverNodeId = peerNode,
                payload = "hi".toByteArray(),
                status = MessageStatus.DELIVERED
            )
        )
        val result = viewModel.conversations.first()
        assertEquals(1, result.size)
        assertEquals(peerNode, result[0].contactNodeId)
    }

    @Test
    fun `conversations counts unread incoming messages`() = runTest(testDispatcher) {
        repeat(3) {
            messageRepo.save(
                Message(
                    messageId = UUID.randomUUID(),
                    senderNodeId = peerNode,
                    receiverNodeId = myNode,
                    payload = "msg".toByteArray(),
                    status = MessageStatus.DELIVERED,
                    isRead = false
                )
            )
        }
        val result = viewModel.conversations.first()
        assertEquals(3, result[0].unreadCount)
    }

    @Test
    fun `conversations marks last message as outgoing when sent by own node`() = runTest(testDispatcher) {
        messageRepo.save(
            Message(
                messageId = UUID.randomUUID(),
                senderNodeId = myNode,
                receiverNodeId = peerNode,
                payload = "hello".toByteArray(),
                status = MessageStatus.DELIVERED
            )
        )
        val result = viewModel.conversations.first()
        assertTrue(result[0].isLastMessageOutgoing)
    }
}
