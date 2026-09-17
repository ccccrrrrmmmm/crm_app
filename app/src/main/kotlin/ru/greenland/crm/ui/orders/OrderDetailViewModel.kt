package ru.greenland.crm.ui.orders

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.greenland.crm.data.local.entity.ClientEntity
import ru.greenland.crm.data.local.entity.MasterEntity
import ru.greenland.crm.data.local.entity.MessageEntity
import ru.greenland.crm.data.local.entity.OrderEntity
import ru.greenland.crm.data.local.entity.OrderPhotoEntity
import ru.greenland.crm.data.local.entity.OrderStatus
import ru.greenland.crm.data.repository.ClientRepository
import ru.greenland.crm.data.repository.MasterRepository
import ru.greenland.crm.data.repository.MessageRepository
import ru.greenland.crm.data.repository.OrderPhotoRepository
import ru.greenland.crm.data.repository.OrderRepository

data class OrderDetailUiState(
    val order: OrderEntity? = null,
    val client: ClientEntity? = null,
    val master: MasterEntity? = null,
    val masters: List<MasterEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val photos: List<OrderPhotoEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val clientRepository: ClientRepository,
    private val masterRepository: MasterRepository,
    private val messageRepository: MessageRepository,
    private val orderPhotoRepository: OrderPhotoRepository,
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val orderFlow = orderRepository.observeById(orderId)

    val uiState: StateFlow<OrderDetailUiState> = orderFlow.flatMapLatest { order ->
        val clientFlow = if (order != null) clientRepository.observeById(order.clientId) else flowOf(null)
        combine(
            clientFlow,
            masterRepository.observeAll(),
            messageRepository.observeByOrder(orderId),
            orderPhotoRepository.observeByOrder(orderId),
        ) { client, masters, messages, photos ->
            OrderDetailUiState(
                order = order,
                client = client,
                master = masters.find { it.id == order?.masterId },
                masters = masters,
                messages = messages,
                photos = photos,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderDetailUiState())

    val draftText = MutableStateFlow("")

    fun onDraftChange(value: String) {
        draftText.value = value
    }

    fun setStatus(status: OrderStatus) {
        val order = uiState.value.order ?: return
        viewModelScope.launch { orderRepository.updateStatus(order, status) }
    }

    fun assignMaster(masterId: String?) {
        val order = uiState.value.order ?: return
        viewModelScope.launch {
            orderRepository.update(
                existing = order,
                clientId = order.clientId,
                masterId = masterId,
                title = order.title,
                category = order.category,
                description = order.description,
                materialsNote = order.materialsNote,
                price = order.price,
                startedAt = order.startedAt,
                finishedAt = order.finishedAt,
            )
        }
    }

    fun sendMessage() {
        val text = draftText.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            messageRepository.send(orderId = orderId, fromMaster = true, text = text)
            draftText.value = ""
        }
    }

    /** Создаёт файл для нового снимка и возвращает его content-Uri для интента камеры. */
    fun preparePhotoCapture(): Pair<File, Uri> {
        val file = orderPhotoRepository.createPhotoFile()
        return file to orderPhotoRepository.uriForFile(file)
    }

    fun confirmPhotoCaptured(file: File) {
        viewModelScope.launch { orderPhotoRepository.attach(orderId, file) }
    }

    fun deletePhoto(photo: OrderPhotoEntity) {
        viewModelScope.launch { orderPhotoRepository.delete(photo) }
    }

    fun deleteOrder(onDeleted: () -> Unit) {
        val order = uiState.value.order ?: return
        viewModelScope.launch {
            orderRepository.delete(order)
            onDeleted()
        }
    }
}
