package ru.greenland.crm.ui.orders

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.greenland.crm.data.local.entity.OrderStatus

private const val PREFS_NAME = "orders_tab_order"
private const val KEY_ORDER = "status_order"

/**
 * Порядок вкладок-статусов на экране заявок — тот, что мастер настроил перетаскиванием.
 * Секретов здесь нет, поэтому обычный SharedPreferences (в отличие от токена синхронизации).
 * По умолчанию первой стоит «В работе» — самый частый рабочий фильтр.
 */
@Singleton
class StatusTabOrderStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _order = MutableStateFlow(load())
    val order: StateFlow<List<OrderStatus>> = _order.asStateFlow()

    fun setOrder(order: List<OrderStatus>) {
        val normalized = normalize(order)
        prefs.edit().putString(KEY_ORDER, normalized.joinToString(",") { it.name }).apply()
        _order.value = normalized
    }

    private fun load(): List<OrderStatus> {
        val stored = prefs.getString(KEY_ORDER, null) ?: return DEFAULT_ORDER
        val parsed = stored.split(",").mapNotNull { name ->
            runCatching { OrderStatus.valueOf(name) }.getOrNull()
        }
        return if (parsed.isEmpty()) DEFAULT_ORDER else normalize(parsed)
    }

    /**
     * Гарантирует, что в списке ровно все статусы: дубликаты и неизвестные значения выкидываем,
     * а недостающие (например, статус, добавленный в новой версии приложения) дописываем в конец.
     */
    private fun normalize(order: List<OrderStatus>): List<OrderStatus> {
        val result = LinkedHashSet(order)
        result.addAll(DEFAULT_ORDER)
        return result.toList()
    }

    companion object {
        /** «В работе» первой, дальше — по жизненному циклу заявки. */
        val DEFAULT_ORDER = listOf(
            OrderStatus.IN_PROGRESS,
            OrderStatus.NEW,
            OrderStatus.DONE,
            OrderStatus.CANCELLED,
        )
    }
}
