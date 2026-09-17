package ru.greenland.crm.data.local.entity

import androidx.room.Entity

/**
 * Учёт синхронизации с GitHub по одному файлу репозитория (по одной записи — клиент,
 * заявка, сообщение, фото...). [path] — путь файла в репозитории, он же ключ.
 *
 * [remoteSha] — sha блоба, который мы последний раз отправили или получили для этого пути;
 * по нему на pull определяем, менялся ли файл на GitHub с прошлого раза.
 * [pushedLocalUpdatedAt] — значение `updatedAt` записи на момент последней успешной отправки;
 * если текущий `updatedAt` записи больше — значит, есть локальные изменения, которые ещё не ушли.
 */
@Entity(tableName = "sync_state", primaryKeys = ["path"])
data class SyncStateEntity(
    val path: String,
    val remoteSha: String,
    val pushedLocalUpdatedAt: Long,
)
