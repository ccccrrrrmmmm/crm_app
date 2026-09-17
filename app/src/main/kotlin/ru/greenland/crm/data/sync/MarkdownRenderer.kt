package ru.greenland.crm.data.sync

import ru.greenland.crm.data.local.entity.ClientEntity
import ru.greenland.crm.data.local.entity.MasterEntity
import ru.greenland.crm.data.local.entity.OrderEntity
import ru.greenland.crm.ui.components.formatDateTime

/**
 * Человекочитаемые md-версии записей — чтобы заявку или клиента можно было нормально
 * прочитать прямо на github.com, а не разбирать JSON. Источник истины для синхронизации —
 * всё равно JSON рядом; из md обратно ничего не парсится.
 */
object MarkdownRenderer {

    fun client(entity: ClientEntity): String = buildString {
        if (entity.isDeleted) {
            appendLine("# ~~${entity.fullName}~~ (удалён)")
            return@buildString
        }
        appendLine("# ${entity.fullName}")
        appendLine()
        appendLine("- Телефон: ${entity.phone}")
        entity.address?.takeIf { it.isNotBlank() }?.let { appendLine("- Адрес: $it") }
        appendLine("- Добавлен: ${formatDateTime(entity.createdAt)}")
        appendLine("- Обновлён: ${formatDateTime(entity.updatedAt)}")
        entity.note?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine("## Заметка")
            appendLine(it)
        }
    }

    fun master(entity: MasterEntity): String = buildString {
        if (entity.isDeleted) {
            appendLine("# ~~${entity.fullName}~~ (удалён)")
            return@buildString
        }
        appendLine("# ${entity.fullName}")
        appendLine()
        appendLine("- Телефон: ${entity.phone}")
        entity.specialization?.takeIf { it.isNotBlank() }?.let { appendLine("- Специализация: $it") }
        appendLine("- Активен: ${if (entity.active) "да" else "нет"}")
        appendLine("- Обновлён: ${formatDateTime(entity.updatedAt)}")
    }

    fun order(entity: OrderEntity, clientName: String?, masterName: String?): String = buildString {
        if (entity.isDeleted) {
            appendLine("# ~~${entity.title}~~ (удалена)")
            return@buildString
        }
        appendLine("# ${entity.title}")
        appendLine()
        appendLine("- Клиент: ${clientName ?: "—"}")
        appendLine("- Тип работ: ${entity.category}")
        appendLine("- Статус: ${entity.status.label}")
        appendLine("- Мастер: ${masterName ?: "не назначен"}")
        entity.price?.let { appendLine("- Стоимость: ${it.toInt()} ₽") }
        entity.startedAt?.let { appendLine("- Время начала: ${formatDateTime(it)}") }
        entity.finishedAt?.let { appendLine("- Время окончания: ${formatDateTime(it)}") }
        appendLine("- Создана: ${formatDateTime(entity.createdAt)}")
        appendLine("- Обновлена: ${formatDateTime(entity.updatedAt)}")

        entity.description?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine("## Комментарий")
            appendLine(it)
        }
        entity.materialsNote?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine("## Материалы")
            appendLine(it)
        }
    }
}
