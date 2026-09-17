package ru.greenland.crm.data.repository

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ru.greenland.crm.data.local.dao.MessageDao
import ru.greenland.crm.data.local.entity.MessageEntity
import ru.greenland.crm.data.sync.GitHubSyncScheduler

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val syncScheduler: GitHubSyncScheduler,
) {
    fun observeByOrder(orderId: String): Flow<List<MessageEntity>> = messageDao.observeByOrder(orderId)

    /** Пишет сообщение в локальную БД мгновенно — работает без интернета, потом уйдёт в GitHub. */
    suspend fun send(orderId: String, fromMaster: Boolean, text: String) {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            orderId = orderId,
            fromMaster = fromMaster,
            text = text,
            createdAt = System.currentTimeMillis(),
        )
        messageDao.upsert(message)
        syncScheduler.triggerSoon()
    }
}
