package ru.greenland.crm.ui.clients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.greenland.crm.data.local.entity.ClientEntity
import ru.greenland.crm.data.local.entity.OrderEntity
import ru.greenland.crm.data.repository.ClientRepository
import ru.greenland.crm.data.repository.OrderRepository

data class ClientDetailUiState(
    val client: ClientEntity? = null,
    val orders: List<OrderEntity> = emptyList(),
)

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clientRepository: ClientRepository,
    orderRepository: OrderRepository,
) : ViewModel() {

    private val clientId: String = checkNotNull(savedStateHandle["clientId"])

    val uiState: StateFlow<ClientDetailUiState> = combine(
        clientRepository.observeById(clientId),
        orderRepository.observeByClient(clientId),
    ) { client, orders ->
        ClientDetailUiState(client = client, orders = orders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientDetailUiState())

    fun deleteClient(onDeleted: () -> Unit) {
        val client = uiState.value.client ?: return
        viewModelScope.launch {
            clientRepository.delete(client)
            onDeleted()
        }
    }
}
