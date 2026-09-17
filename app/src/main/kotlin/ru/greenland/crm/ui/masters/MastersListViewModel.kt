package ru.greenland.crm.ui.masters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.greenland.crm.data.local.entity.MasterEntity
import ru.greenland.crm.data.repository.MasterRepository

@HiltViewModel
class MastersListViewModel @Inject constructor(
    private val masterRepository: MasterRepository,
) : ViewModel() {

    val masters: StateFlow<List<MasterEntity>> = masterRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setActive(master: MasterEntity, active: Boolean) {
        viewModelScope.launch {
            masterRepository.update(
                existing = master,
                fullName = master.fullName,
                phone = master.phone,
                specialization = master.specialization,
                active = active,
            )
        }
    }
}
