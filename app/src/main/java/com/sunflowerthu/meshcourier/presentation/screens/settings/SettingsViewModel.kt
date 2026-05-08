package com.sunflowerthu.meshcourier.presentation.screens.settings

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.sunflowerthu.meshcourier.domain.crypto.CryptoManager
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.usecase.GetOwnNodeIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.Security
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getOwnNodeIdUseCase: GetOwnNodeIdUseCase,
    private val cryptoManager: CryptoManager,
) : ViewModel() {

    val myNodeId: NodeId = getOwnNodeIdUseCase.execute()

    private val _hasKeyPair = MutableStateFlow(cryptoManager.hasOwnKeyPair())
    val hasKeyPair = _hasKeyPair.asStateFlow()

    private val _keyGenError = MutableStateFlow<String?>(null)
    val keyGenError = _keyGenError.asStateFlow()

    // QR содержит nodeId|base64(publicKey) если ключи есть, иначе только nodeId
    val qrContent = _hasKeyPair.map { hasKey ->
        if (hasKey) {
            runCatching {
                val encoded = Base64.encodeToString(
                    cryptoManager.getPublicKeyEncoded(), Base64.NO_WRAP
                )
                "${myNodeId.value}|$encoded"
            }.getOrDefault(myNodeId.value)
        } else myNodeId.value
    }.stateIn(viewModelScope, SharingStarted.Eagerly, myNodeId.value)

    fun generateKeyPair() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { cryptoManager.generateOwnKeyPair() }
                .onSuccess { _hasKeyPair.value = true }
                .onFailure { e ->
                    Log.e(TAG, "Генерация ключей провалилась", e)
                    _keyGenError.value = e.message ?: e.javaClass.simpleName
                }
        }
    }

    fun clearKeyGenError() { _keyGenError.value = null }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
