package ru.greenland.crm.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import ru.greenland.crm.data.local.dao.ClientDao
import ru.greenland.crm.data.local.dao.MasterDao
import ru.greenland.crm.data.local.dao.MessageDao
import ru.greenland.crm.data.local.dao.OrderDao
import ru.greenland.crm.data.local.dao.OrderPhotoDao
import ru.greenland.crm.data.local.dao.SyncStateDao
import ru.greenland.crm.data.local.entity.ClientEntity
import ru.greenland.crm.data.local.entity.MasterEntity
import ru.greenland.crm.data.local.entity.MessageEntity
import ru.greenland.crm.data.local.entity.OrderEntity
import ru.greenland.crm.data.local.entity.OrderPhotoEntity
import ru.greenland.crm.data.local.entity.SyncStateEntity
import ru.greenland.crm.data.remote.github.GitHubApi
import ru.greenland.crm.data.remote.github.GitHubTreeEntry

/**
 * Оркестратор синхронизации с GitHub: сначала отправляет всё, что изменилось локально
 * (по одному файлу на запись), потом скачивает дерево репозитория и подмешивает то, что
 * изменилось снаружи. Конфликты решаются по `updatedAt` — чья запись свежее, та и побеждает.
 */
@Singleton
class GitHubSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: GitHubApi,
    private val settingsStore: GitHubSettingsStore,
    private val json: Json,
    private val clientDao: ClientDao,
    private val masterDao: MasterDao,
    private val orderDao: OrderDao,
    private val messageDao: MessageDao,
    private val photoDao: OrderPhotoDao,
    private val syncStateDao: SyncStateDao,
) {
    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    suspend fun syncNow(): Result<Unit> {
        val settings = settingsStore.current()
        if (!settings.isConfigured) return Result.success(Unit)

        _status.value = _status.value.copy(isSyncing = true, lastError = null)
        return try {
            pushAll()
            pullAll()
            _status.value = SyncStatus(isSyncing = false, lastSuccessAt = System.currentTimeMillis())
            Result.success(Unit)
        } catch (t: Throwable) {
            _status.value = _status.value.copy(isSyncing = false, lastError = t.message ?: t.toString())
            Result.failure(t)
        }
    }

    // ---- отправка локальных изменений ----

    private suspend fun pushAll() {
        clientDao.getAllForSync().forEach {
            pushRecord(RepoPaths.client(it.id), it.updatedAt, ClientEntity.serializer(), it)
            pushMarkdown(RepoPaths.clientDoc(it.id), it.updatedAt, MarkdownRenderer.client(it))
        }
        masterDao.getAllForSync().forEach {
            pushRecord(RepoPaths.master(it.id), it.updatedAt, MasterEntity.serializer(), it)
            pushMarkdown(RepoPaths.masterDoc(it.id), it.updatedAt, MarkdownRenderer.master(it))
        }
        orderDao.getAllForSync().forEach { order ->
            pushRecord(RepoPaths.order(order.id), order.updatedAt, OrderEntity.serializer(), order)
            val clientName = clientDao.getById(order.clientId)?.fullName
            val masterName = order.masterId?.let { masterDao.getById(it)?.fullName }
            pushMarkdown(RepoPaths.orderDoc(order.id), order.updatedAt, MarkdownRenderer.order(order, clientName, masterName))
        }
        messageDao.getAllForSync().forEach { pushRecord(RepoPaths.message(it.orderId, it.id), it.createdAt, MessageEntity.serializer(), it) }
        photoDao.getAllForSync().forEach { pushPhoto(it) }
    }

    private suspend fun <T> pushRecord(path: String, localUpdatedAt: Long, serializer: KSerializer<T>, value: T) {
        val state = syncStateDao.getByPath(path)
        if (state != null && state.pushedLocalUpdatedAt >= localUpdatedAt) return
        val bytes = json.encodeToString(serializer, value).toByteArray()
        val sha = api.putFile(path, bytes, commitMessage(path), state?.remoteSha)
        syncStateDao.upsert(SyncStateEntity(path, sha, localUpdatedAt))
    }

    private suspend fun pushMarkdown(path: String, localUpdatedAt: Long, content: String) {
        val state = syncStateDao.getByPath(path)
        if (state != null && state.pushedLocalUpdatedAt >= localUpdatedAt) return
        val sha = api.putFile(path, content.toByteArray(), commitMessage(path), state?.remoteSha)
        syncStateDao.upsert(SyncStateEntity(path, sha, localUpdatedAt))
    }

    private suspend fun pushPhoto(photo: OrderPhotoEntity) {
        val metaPath = RepoPaths.photoMeta(photo.orderId, photo.id)
        val state = syncStateDao.getByPath(metaPath)
        if (state != null && state.pushedLocalUpdatedAt >= photo.updatedAt) return

        if (!photo.isDeleted) {
            val binPath = RepoPaths.photoBinary(photo.orderId, photo.id)
            val file = File(photo.filePath)
            if (file.exists() && syncStateDao.getByPath(binPath) == null) {
                val sha = api.putFile(binPath, file.readBytes(), commitMessage(binPath), null)
                syncStateDao.upsert(SyncStateEntity(binPath, sha, photo.updatedAt))
            }
        }

        val bytes = json.encodeToString(OrderPhotoEntity.serializer(), photo).toByteArray()
        val sha = api.putFile(metaPath, bytes, commitMessage(metaPath), state?.remoteSha)
        syncStateDao.upsert(SyncStateEntity(metaPath, sha, photo.updatedAt))
    }

    private fun commitMessage(path: String) = "sync: $path"

    // ---- получение внешних изменений ----

    private suspend fun pullAll() {
        val tree = api.getTree()
        val byPath = tree.associateBy { it.path }

        pullCollection(byPath, "data/clients/") { bytes -> mergeClient(json.decodeFromString(ClientEntity.serializer(), bytes)) }
        pullCollection(byPath, "data/masters/") { bytes -> mergeMaster(json.decodeFromString(MasterEntity.serializer(), bytes)) }
        pullCollection(byPath, "data/orders/") { bytes -> mergeOrder(json.decodeFromString(OrderEntity.serializer(), bytes)) }
        pullCollection(byPath, "data/messages/") { bytes -> mergeMessage(json.decodeFromString(MessageEntity.serializer(), bytes)) }
        pullPhotos(byPath)
    }

    private suspend fun pullCollection(
        byPath: Map<String, GitHubTreeEntry>,
        prefix: String,
        merge: suspend (String) -> Long,
    ) {
        for ((path, entry) in byPath) {
            if (!path.startsWith(prefix) || !path.endsWith(".json")) continue
            if (syncStateDao.getRemoteSha(path) == entry.sha) continue
            val resultingUpdatedAt = merge(String(api.getBlob(entry.sha)))
            syncStateDao.upsert(SyncStateEntity(path, entry.sha, resultingUpdatedAt))
        }
    }

    private suspend fun mergeClient(remote: ClientEntity): Long {
        val local = clientDao.getById(remote.id)
        if (local != null && local.updatedAt > remote.updatedAt) return local.updatedAt
        clientDao.upsert(remote)
        return remote.updatedAt
    }

    private suspend fun mergeMaster(remote: MasterEntity): Long {
        val local = masterDao.getById(remote.id)
        if (local != null && local.updatedAt > remote.updatedAt) return local.updatedAt
        masterDao.upsert(remote)
        return remote.updatedAt
    }

    private suspend fun mergeOrder(remote: OrderEntity): Long {
        val local = orderDao.getById(remote.id)
        if (local != null && local.updatedAt > remote.updatedAt) return local.updatedAt
        orderDao.upsert(remote)
        return remote.updatedAt
    }

    private suspend fun mergeMessage(remote: MessageEntity): Long {
        messageDao.upsert(remote) // переписка только дополняется, конфликтов по содержанию нет
        return remote.createdAt
    }

    private suspend fun pullPhotos(byPath: Map<String, GitHubTreeEntry>) {
        for ((path, entry) in byPath) {
            if (!path.startsWith("data/photos/") || !path.endsWith(".json")) continue
            if (syncStateDao.getRemoteSha(path) == entry.sha) continue
            val remote = json.decodeFromString(OrderPhotoEntity.serializer(), String(api.getBlob(entry.sha)))
            val resultingUpdatedAt = mergePhotoMeta(remote, byPath)
            syncStateDao.upsert(SyncStateEntity(path, entry.sha, resultingUpdatedAt))
        }
    }

    private suspend fun mergePhotoMeta(remote: OrderPhotoEntity, byPath: Map<String, GitHubTreeEntry>): Long {
        val local = photoDao.getById(remote.id)
        if (local != null && local.updatedAt > remote.updatedAt) return local.updatedAt

        if (remote.isDeleted) {
            local?.let { File(it.filePath).delete() }
            photoDao.upsert(remote)
            return remote.updatedAt
        }

        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val targetFile = File(photosDir, "${remote.id}.jpg")
        if (!targetFile.exists()) {
            val binPath = RepoPaths.photoBinary(remote.orderId, remote.id)
            byPath[binPath]?.let { binEntry ->
                targetFile.writeBytes(api.getBlob(binEntry.sha))
                syncStateDao.upsert(SyncStateEntity(binPath, binEntry.sha, remote.updatedAt))
            }
        }
        photoDao.upsert(remote.copy(filePath = targetFile.absolutePath))
        return remote.updatedAt
    }
}
