package ru.greenland.crm.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.greenland.crm.data.local.entity.ClientEntity
import ru.greenland.crm.data.repository.ClientRepository

data class ClientsUiState(val clients: List<ClientEntity> = emptyList(), val query: String = "")

@HiltViewModel
class ClientsListViewModel @Inject constructor(
    clientRepository: ClientRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<ClientsUiState> = combine(
        clientRepository.observeAll(),
        query,
    ) { clients, q ->
        val filtered = if (q.isBlank()) {
            clients
        } else {
            clients.filter { it.fullName.contains(q, ignoreCase = true) || it.phone.contains(q) }
        }
        ClientsUiState(clients = filtered, query = q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientsUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }
}
