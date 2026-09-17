package ru.greenland.crm.data.local.entity

import kotlinx.serialization.Serializable

/** Статус заявки в её жизненном цикле. */
@Serializable
enum class OrderStatus(val label: String) {
    NEW("Новая"),
    IN_PROGRESS("В работе"),
    DONE("Выполнена"),
    CANCELLED("Отменена"),
}
