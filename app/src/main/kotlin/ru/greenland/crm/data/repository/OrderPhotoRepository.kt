package ru.greenland.crm.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.greenland.crm.data.local.dao.OrderPhotoDao
import ru.greenland.crm.data.local.entity.OrderPhotoEntity
import ru.greenland.crm.data.sync.GitHubSyncScheduler

/**
 * Фото по заявке хранятся как обычные файлы в приватном хранилище приложения
 * (`filesDir/photos`) — не нужны разрешения на хранилище, всё работает офлайн.
 * Камере системы файл отдаётся через [FileProvider], т.к. напрямую file:// Uri
 * сторонним приложениям передавать нельзя.
 */
@Singleton
class OrderPhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val orderPhotoDao: OrderPhotoDao,
    private val syncScheduler: GitHubSyncScheduler,
) {
    fun observeByOrder(orderId: String): Flow<List<OrderPhotoEntity>> = orderPhotoDao.observeByOrder(orderId)

    fun createPhotoFile(): File {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    fun uriForFile(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Копирует фото, выбранное из галереи (content:// Uri), в собственное хранилище приложения. */
    suspend fun attachFromUri(orderId: String, uri: Uri) {
        val file = createPhotoFile()
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Не удалось прочитать выбранное фото")
        }
        attach(orderId, file)
    }

    suspend fun attach(orderId: String, file: File) {
        val now = System.currentTimeMillis()
        orderPhotoDao.upsert(
            OrderPhotoEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                filePath = file.absolutePath,
                createdAt = now,
                updatedAt = now,
            ),
        )
        syncScheduler.triggerSoon()
    }

    /** Мягкое удаление — локальный файл убираем сразу, а «надгробие» уходит в GitHub, чтобы фото пропало и там. */
    suspend fun delete(photo: OrderPhotoEntity) {
        File(photo.filePath).delete()
        orderPhotoDao.upsert(photo.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        syncScheduler.triggerSoon()
    }
}
