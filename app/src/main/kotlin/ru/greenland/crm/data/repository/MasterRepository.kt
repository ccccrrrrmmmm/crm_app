package ru.greenland.crm.data.repository

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ru.greenland.crm.data.local.dao.MasterDao
import ru.greenland.crm.data.local.entity.MasterEntity
import ru.greenland.crm.data.sync.GitHubSyncScheduler

@Singleton
class MasterRepository @Inject constructor(
    private val masterDao: MasterDao,
    private val syncScheduler: GitHubSyncScheduler,
) {
    fun observeAll(): Flow<List<MasterEntity>> = masterDao.observeAll()

    fun observeById(id: String): Flow<MasterEntity?> = masterDao.observeById(id)

    suspend fun getById(id: String): MasterEntity? = masterDao.getById(id)

    suspend fun create(fullName: String, phone: String, specialization: String?): String {
        val now = System.currentTimeMillis()
        val master = MasterEntity(
            id = UUID.randomUUID().toString(),
            fullName = fullName,
            phone = phone,
            specialization = specialization,
            active = true,
            createdAt = now,
            updatedAt = now,
        )
        masterDao.upsert(master)
        syncScheduler.triggerSoon()
        return master.id
    }

    suspend fun update(existing: MasterEntity, fullName: String, phone: String, specialization: String?, active: Boolean) {
        masterDao.upsert(
            existing.copy(
                fullName = fullName,
                phone = phone,
                specialization = specialization,
                active = active,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        syncScheduler.triggerSoon()
    }

    suspend fun delete(master: MasterEntity) {
        masterDao.upsert(master.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        syncScheduler.triggerSoon()
    }
}
