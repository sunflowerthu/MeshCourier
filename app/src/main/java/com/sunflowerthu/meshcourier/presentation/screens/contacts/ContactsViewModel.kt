package com.sunflowerthu.meshcourier.presentation.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflowerthu.meshcourier.domain.models.NodeId
import com.sunflowerthu.meshcourier.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
) : ViewModel() {

    val contacts = contactRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun delete(nodeId: NodeId) {
        viewModelScope.launch {
            runCatching { contactRepository.delete(nodeId) }
        }
    }
}
