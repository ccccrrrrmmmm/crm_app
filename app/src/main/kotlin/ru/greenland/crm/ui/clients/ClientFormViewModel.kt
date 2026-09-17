package ru.greenland.crm.ui.clients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.greenland.crm.data.repository.ClientRepository

data class ClientFormState(
    val isEditing: Boolean = false,
    val fullName: String = "",
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val saved: Boolean = false,
    val savedClientId: String? = null,
)

@HiltViewModel
class ClientFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clientRepository: ClientRepository,
) : ViewModel() {

    private val clientId: String? = savedStateHandle["clientId"]

    private val _uiState = MutableStateFlow(ClientFormState(isEditing = clientId != null))
    val uiState: StateFlow<ClientFormState> = _uiState.asStateFlow()

    init {
        if (clientId != null) {
            viewModelScope.launch {
                clientRepository.getById(clientId)?.let { client ->
                    _uiState.value = _uiState.value.copy(
                        fullName = client.fullName,
                        phone = client.phone,
                        address = client.address.orEmpty(),
                        note = client.note.orEmpty(),
                    )
                }
            }
        }
    }

    fun onFullNameChange(value: String) { _uiState.value = _uiState.value.copy(fullName = value) }
    fun onPhoneChange(value: String) { _uiState.value = _uiState.value.copy(phone = value) }
    fun onAddressChange(value: String) { _uiState.value = _uiState.value.copy(address = value) }
    fun onNoteChange(value: String) { _uiState.value = _uiState.value.copy(note = value) }

    fun save() {
        val state = _uiState.value
        if (state.fullName.isBlank() || state.phone.isBlank()) return

        viewModelScope.launch {
            val id = if (clientId != null) {
                clientRepository.getById(clientId)?.let { existing ->
                    clientRepository.update(
                        existing = existing,
                        fullName = state.fullName.trim(),
                        phone = state.phone.trim(),
                        address = state.address.ifBlank { null },
                        note = state.note.ifBlank { null },
                    )
                }
                clientId
            } else {
                clientRepository.create(
                    fullName = state.fullName.trim(),
                    phone = state.phone.trim(),
                    address = state.address.ifBlank { null },
                    note = state.note.ifBlank { null },
                )
            }
            _uiState.value = _uiState.value.copy(saved = true, savedClientId = id)
        }
    }
}
