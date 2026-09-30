package ru.greenland.crm.ui.orders

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
import ru.greenland.crm.data.local.entity.MasterEntity
import ru.greenland.crm.data.local.entity.OrderEntity
import ru.greenland.crm.data.local.entity.OrderStatus
import ru.greenland.crm.data.repository.ClientRepository
import ru.greenland.crm.data.repository.MasterRepository
import ru.greenland.crm.data.repository.OrderRepository

data class OrderListItem(
    val order: OrderEntity,
    val clientName: String,
    val masterName: String?,
)

data class OrdersUiState(
    val items: List<OrderListItem> = emptyList(),
    val filter: OrderStatus? = OrderStatus.IN_PROGRESS,
    val statusOrder: List<OrderStatus> = StatusTabOrderStore.DEFAULT_ORDER,
)

@HiltViewModel
class OrdersListViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    clientRepository: ClientRepository,
    masterRepository: MasterRepository,
    private val tabOrderStore: StatusTabOrderStore,
) : ViewModel() {

    // По умолчанию открываемся сразу на «В работе» — мастеру не нужно самому переключаться.
    private val filter = MutableStateFlow<OrderStatus?>(OrderStatus.IN_PROGRESS)

    val uiState: StateFlow<OrdersUiState> = combine(
        orderRepository.observeAll(),
        clientRepository.observeAll(),
        masterRepository.observeAll(),
        filter,
        tabOrderStore.order,
    ) { orders, clients, masters, currentFilter, statusOrder ->
        val clientNames = clients.associateBy(ClientEntity::id)
        val masterNames = masters.associateBy(MasterEntity::id)
        val filtered = if (currentFilter == null) orders else orders.filter { it.status == currentFilter }
        OrdersUiState(
            items = filtered.map { order ->
                OrderListItem(
                    order = order,
                    clientName = clientNames[order.clientId]?.fullName ?: "Клиент удалён",
                    masterName = order.masterId?.let { masterNames[it]?.fullName },
                )
            },
            filter = currentFilter,
            statusOrder = statusOrder,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrdersUiState())

    fun setFilter(status: OrderStatus?) {
        filter.value = status
    }

    /** Сохраняет новый порядок вкладок-статусов после перетаскивания. */
    fun setStatusOrder(order: List<OrderStatus>) {
        tabOrderStore.setOrder(order)
    }
}
