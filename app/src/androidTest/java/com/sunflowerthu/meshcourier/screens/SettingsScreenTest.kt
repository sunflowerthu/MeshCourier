package com.sunflowerthu.meshcourier.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.models.NodeIdentity
import com.sunflowerthu.meshcourier.domain.usecase.GetOwnNodeIdUseCase
import com.sunflowerthu.meshcourier.mocks.MockCryptoManager
import com.sunflowerthu.meshcourier.presentation.screens.settings.SettingsScreen
import com.sunflowerthu.meshcourier.presentation.screens.settings.SettingsViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val myNode = NodeId("test-node-id")

    private fun buildViewModel(hasKey: Boolean = false): SettingsViewModel {
        val crypto = MockCryptoManager().apply { hasOwnKey = hasKey }
        return SettingsViewModel(
            getOwnNodeIdUseCase = GetOwnNodeIdUseCase(NodeIdentity(myNode)),
            cryptoManager = crypto
        )
    }

    @Test
    fun shows_settings_title() {
        composeRule.setContent {
            SettingsScreen(onOpenContacts = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText("Настройки").assertIsDisplayed()
    }

    @Test
    fun shows_my_node_id_section_title() {
        composeRule.setContent {
            SettingsScreen(onOpenContacts = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText("Мой Node ID").assertIsDisplayed()
    }

    @Test
    fun shows_node_id_value() {
        composeRule.setContent {
            SettingsScreen(onOpenContacts = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText(myNode.value).assertIsDisplayed()
    }

    @Test
    fun shows_keys_missing_status_when_no_key_pair() {
        composeRule.setContent {
            SettingsScreen(onOpenContacts = {}, viewModel = buildViewModel(hasKey = false))
        }
        composeRule.onNodeWithText("Ключи не созданы").assertIsDisplayed()
    }

    @Test
    fun shows_generate_keys_button_when_no_key_pair() {
        composeRule.setContent {
            SettingsScreen(onOpenContacts = {}, viewModel = buildViewModel(hasKey = false))
        }
        composeRule.onNodeWithText("Сгенерировать ключи").assertIsDisplayed()
    }

    @Test
    fun shows_keys_generated_status_when_key_pair_present() {
        composeRule.setContent {
            SettingsScreen(onOpenContacts = {}, viewModel = buildViewModel(hasKey = true))
        }
        composeRule.onNodeWithText("Ключи сгенерированы").assertIsDisplayed()
    }

    @Test
    fun hides_generate_keys_button_when_key_pair_present() {
        composeRule.setContent {
            SettingsScreen(onOpenContacts = {}, viewModel = buildViewModel(hasKey = true))
        }
        composeRule.onNodeWithText("Сгенерировать ключи").assertDoesNotExist()
    }
}
