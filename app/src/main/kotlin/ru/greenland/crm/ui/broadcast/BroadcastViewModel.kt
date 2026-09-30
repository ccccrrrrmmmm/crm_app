package ru.greenland.crm.ui.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.greenland.crm.data.local.entity.ClientEntity
import ru.greenland.crm.data.repository.ClientRepository

data class BroadcastUiState(
    val message: String = "",
    val clients: List<ClientEntity> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    // Пользователь хоть раз трогал галочки — значит не перетираем его выбор при обновлении списка.
    val selectionTouched: Boolean = false,
    // Прогресс и итог автоматической SMS-рассылки.
    val sending: Boolean = false,
    val done: Boolean = false,
    val sent: Int = 0,
    val failed: Int = 0,
    val total: Int = 0,
) {
    /** Клиенты, которым в принципе можно написать (есть телефон). */
    val reachable: List<ClientEntity> get() = clients.filter { it.phone.isNotBlank() }

    /** Сколько выбрано среди тех, кому можно написать. */
    val selectedCount: Int get() = reachable.count { it.id in selectedIds }

    val allReachableSelected: Boolean get() = reachable.isNotEmpty() && reachable.all { it.id in selectedIds }
}

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    clientRepository: ClientRepository,
    private val smsSender: SmsSender,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BroadcastUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            clientRepository.observeAll().collect { list ->
                _uiState.update { state ->
                    val reachableIds = list.filter { it.phone.isNotBlank() }.map { it.id }.toSet()
                    state.copy(
                        clients = list,
                        // По умолчанию выбраны все с телефоном; если пользователь уже правил выбор —
                        // сохраняем его, только выкидывая тех, кто пропал из базы.
                        selectedIds = if (state.selectionTouched) {
                            state.selectedIds intersect reachableIds
                        } else {
                            reachableIds
                        },
                    )
                }
            }
        }
    }

    fun onMessageChange(value: String) = _uiState.update { it.copy(message = value) }

    fun toggle(id: String) = _uiState.update { state ->
        val next = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
        state.copy(selectedIds = next, selectionTouched = true)
    }

    fun toggleAll() = _uiState.update { state ->
        val next = if (state.allReachableSelected) emptySet() else state.reachable.map { it.id }.toSet()
        state.copy(selectedIds = next, selectionTouched = true)
    }

    /**
     * Автоматическая SMS-рассылка: проходит по всем выбранным получателям и отправляет каждому
     * персональный текст без дальнейшего участия пользователя. Разрешение SEND_SMS должно быть
     * уже выдано (об этом заботится экран).
     */
    fun start() {
        val snapshot = _uiState.value
        val queue = snapshot.reachable.filter { it.id in snapshot.selectedIds }
        if (queue.isEmpty() || snapshot.message.isBlank()) return

        _uiState.update {
            it.copy(sending = true, done = false, sent = 0, failed = 0, total = queue.size)
        }
        viewModelScope.launch {
            var sent = 0
            var failed = 0
            for (client in queue) {
                val number = normalizePhoneForSms(client.phone)
                val ok = number != null && withContext(Dispatchers.IO) {
                    smsSender.send(number, renderBroadcast(snapshot.message, client.fullName))
                }
                if (ok) sent++ else failed++
                _uiState.update { it.copy(sent = sent, failed = failed) }
            }
            _uiState.update { it.copy(sending = false, done = true) }
        }
    }

    fun reset() = _uiState.update {
        it.copy(sending = false, done = false, sent = 0, failed = 0, total = 0)
    }
}
