package ru.greenland.crm.data.repository

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ru.greenland.crm.data.local.dao.ClientDao
import ru.greenland.crm.data.local.entity.ClientEntity
import ru.greenland.crm.data.sync.GitHubSyncScheduler

@Singleton
class ClientRepository @Inject constructor(
    private val clientDao: ClientDao,
    private val syncScheduler: GitHubSyncScheduler,
) {
    fun observeAll(): Flow<List<ClientEntity>> = clientDao.observeAll()

    fun observeById(id: String): Flow<ClientEntity?> = clientDao.observeById(id)

    suspend fun getById(id: String): ClientEntity? = clientDao.getById(id)

    suspend fun create(fullName: String, phone: String, address: String?, note: String?): String {
        val now = System.currentTimeMillis()
        val client = ClientEntity(
            id = UUID.randomUUID().toString(),
            fullName = fullName,
            phone = phone,
            address = address,
            note = note,
            createdAt = now,
            updatedAt = now,
        )
        clientDao.upsert(client)
        syncScheduler.triggerSoon()
        return client.id
    }

    suspend fun update(existing: ClientEntity, fullName: String, phone: String, address: String?, note: String?) {
        clientDao.upsert(
            existing.copy(
                fullName = fullName,
                phone = phone,
                address = address,
                note = note,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        syncScheduler.triggerSoon()
    }

    /** Мягкое удаление — запись остаётся (помечена) для корректной синхронизации с GitHub. */
    suspend fun delete(client: ClientEntity) {
        clientDao.upsert(client.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        syncScheduler.triggerSoon()
    }
}
