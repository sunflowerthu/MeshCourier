package com.sunflowerthu.meshcourier.presentation

import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.usecase.GetOwnNodeIdUseCase
import com.sunflowerthu.meshcourier.mocks.MockCryptoManager
import com.sunflowerthu.meshcourier.presentation.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val myNode = NodeId("node-xyz")

    private lateinit var crypto: MockCryptoManager
    private lateinit var viewModel: SettingsViewModel

    private fun buildViewModel() = SettingsViewModel(
        getOwnNodeIdUseCase = GetOwnNodeIdUseCase(NodeIdentity(myNode)),
        cryptoManager = crypto
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        crypto = MockCryptoManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hasKeyPair is false when cryptoManager has no key pair`() {
        crypto.hasOwnKey = false
        viewModel = buildViewModel()
        assertFalse(viewModel.hasKeyPair.value)
    }

    @Test
    fun `hasKeyPair is true when cryptoManager already has key pair`() {
        crypto.hasOwnKey = true
        viewModel = buildViewModel()
        assertTrue(viewModel.hasKeyPair.value)
    }

    @Test
    fun `qrContent is nodeId when no key pair`() {
        crypto.hasOwnKey = false
        viewModel = buildViewModel()
        assertEquals(myNode.value, viewModel.qrContent.value)
    }

    @Test
    fun `myNodeId exposes correct node id`() {
        viewModel = buildViewModel()
        assertEquals(myNode, viewModel.myNodeId)
    }

    @Test
    fun `keyGenError is null initially`() {
        viewModel = buildViewModel()
        assertNull(viewModel.keyGenError.value)
    }

    @Test
    fun `clearKeyGenError sets error to null`() {
        viewModel = buildViewModel()
        // Manually set error to simulate a previous failure
        viewModel.generateKeyPair() // triggers, mock throws or not
        viewModel.clearKeyGenError()
        assertNull(viewModel.keyGenError.value)
    }

    @Test
    fun `generateKeyPair on success sets hasKeyPair to true`() {
        crypto.hasOwnKey = false
        viewModel = buildViewModel()
        viewModel.generateKeyPair()
        Thread.sleep(200)
        assertTrue(viewModel.hasKeyPair.value)
    }

    @Test
    fun `generateKeyPair on failure sets keyGenError`() {
        crypto.hasOwnKey = false
        crypto.generateKeyPairError = RuntimeException("CSP init failed")
        viewModel = buildViewModel()
        viewModel.generateKeyPair()
        Thread.sleep(200)
        assertFalse(viewModel.hasKeyPair.value)
        assertNotNull(viewModel.keyGenError.value)
    }
}
