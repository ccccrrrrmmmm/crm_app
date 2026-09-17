package ru.greenland.crm.ui.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.greenland.crm.data.local.entity.ClientEntity
import ru.greenland.crm.data.local.entity.MasterEntity
import ru.greenland.crm.data.repository.ClientRepository
import ru.greenland.crm.data.repository.MasterRepository
import ru.greenland.crm.data.repository.OrderRepository

data class OrderFormState(
    val isEditing: Boolean = false,
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val materialsNote: String = "",
    val price: String = "",
    val clientId: String? = null,
    val masterId: String? = null,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val clients: List<ClientEntity> = emptyList(),
    val masters: List<MasterEntity> = emptyList(),
    val saved: Boolean = false,
)

@HiltViewModel
class OrderFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val clientRepository: ClientRepository,
    private val masterRepository: MasterRepository,
) : ViewModel() {

    private val orderId: String? = savedStateHandle["orderId"]
    private val preselectedClientId: String? = savedStateHandle["clientId"]

    private val form = MutableStateFlow(
        OrderFormState(isEditing = orderId != null, clientId = preselectedClientId),
    )

    val uiState: StateFlow<OrderFormState> = combine(
        form,
        clientRepository.observeAll(),
        masterRepository.observeAll(),
    ) { state, clients, masters ->
        state.copy(clients = clients, masters = masters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    init {
        if (orderId != null) {
            viewModelScope.launch {
                orderRepository.getById(orderId)?.let { order ->
                    form.value = form.value.copy(
                        title = order.title,
                        category = order.category,
                        description = order.description.orEmpty(),
                        materialsNote = order.materialsNote.orEmpty(),
                        price = order.price?.toInt()?.toString().orEmpty(),
                        clientId = order.clientId,
                        masterId = order.masterId,
                        startedAt = order.startedAt,
                        finishedAt = order.finishedAt,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) { form.value = form.value.copy(title = value) }
    fun onCategoryChange(value: String) { form.value = form.value.copy(category = value) }
    fun onDescriptionChange(value: String) { form.value = form.value.copy(description = value) }
    fun onMaterialsNoteChange(value: String) { form.value = form.value.copy(materialsNote = value) }
    fun onPriceChange(value: String) { form.value = form.value.copy(price = value.filter { it.isDigit() }) }
    fun onClientChange(id: String) { form.value = form.value.copy(clientId = id) }
    fun onMasterChange(id: String?) { form.value = form.value.copy(masterId = id) }
    fun onStartedAtChange(millis: Long?) { form.value = form.value.copy(startedAt = millis) }
    fun onFinishedAtChange(millis: Long?) { form.value = form.value.copy(finishedAt = millis) }

    fun save() {
        val state = uiState.value
        val clientId = state.clientId ?: return
        if (state.title.isBlank() || state.category.isBlank()) return
        val price = state.price.toDoubleOrNull()

        viewModelScope.launch {
            if (orderId != null) {
                orderRepository.getById(orderId)?.let { existing ->
                    orderRepository.update(
                        existing = existing,
                        clientId = clientId,
                        masterId = state.masterId,
                        title = state.title.trim(),
                        category = state.category.trim(),
                        description = state.description.ifBlank { null },
                        materialsNote = state.materialsNote.ifBlank { null },
                        price = price,
                        startedAt = state.startedAt,
                        finishedAt = state.finishedAt,
                    )
                }
            } else {
                orderRepository.create(
                    clientId = clientId,
                    masterId = state.masterId,
                    title = state.title.trim(),
                    category = state.category.trim(),
                    description = state.description.ifBlank { null },
                    materialsNote = state.materialsNote.ifBlank { null },
                    price = price,
                    startedAt = state.startedAt,
                    finishedAt = state.finishedAt,
                )
            }
            form.value = form.value.copy(saved = true)
        }
    }
}
