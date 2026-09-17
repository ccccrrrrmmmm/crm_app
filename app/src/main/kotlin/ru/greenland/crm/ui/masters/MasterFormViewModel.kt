package ru.greenland.crm.ui.masters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.greenland.crm.data.repository.MasterRepository

data class MasterFormState(
    val isEditing: Boolean = false,
    val fullName: String = "",
    val phone: String = "",
    val specialization: String = "",
    val saved: Boolean = false,
)

@HiltViewModel
class MasterFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val masterRepository: MasterRepository,
) : ViewModel() {

    private val masterId: String? = savedStateHandle["masterId"]

    private val _uiState = MutableStateFlow(MasterFormState(isEditing = masterId != null))
    val uiState: StateFlow<MasterFormState> = _uiState.asStateFlow()

    init {
        if (masterId != null) {
            viewModelScope.launch {
                masterRepository.getById(masterId)?.let { master ->
                    _uiState.value = _uiState.value.copy(
                        fullName = master.fullName,
                        phone = master.phone,
                        specialization = master.specialization.orEmpty(),
                    )
                }
            }
        }
    }

    fun onFullNameChange(value: String) { _uiState.value = _uiState.value.copy(fullName = value) }
    fun onPhoneChange(value: String) { _uiState.value = _uiState.value.copy(phone = value) }
    fun onSpecializationChange(value: String) { _uiState.value = _uiState.value.copy(specialization = value) }

    fun save() {
        val state = _uiState.value
        if (state.fullName.isBlank() || state.phone.isBlank()) return

        viewModelScope.launch {
            if (masterId != null) {
                masterRepository.getById(masterId)?.let { existing ->
                    masterRepository.update(
                        existing = existing,
                        fullName = state.fullName.trim(),
                        phone = state.phone.trim(),
                        specialization = state.specialization.ifBlank { null },
                        active = existing.active,
                    )
                }
            } else {
                masterRepository.create(
                    fullName = state.fullName.trim(),
                    phone = state.phone.trim(),
                    specialization = state.specialization.ifBlank { null },
                )
            }
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }
}
