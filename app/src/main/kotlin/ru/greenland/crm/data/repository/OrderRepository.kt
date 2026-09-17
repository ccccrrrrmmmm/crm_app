package ru.greenland.crm.data.repository

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ru.greenland.crm.data.local.dao.OrderDao
import ru.greenland.crm.data.local.entity.OrderEntity
import ru.greenland.crm.data.local.entity.OrderStatus
import ru.greenland.crm.data.sync.GitHubSyncScheduler

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val syncScheduler: GitHubSyncScheduler,
) {
    fun observeAll(): Flow<List<OrderEntity>> = orderDao.observeAll()

    fun observeByClient(clientId: String): Flow<List<OrderEntity>> = orderDao.observeByClient(clientId)

    fun observeById(id: String): Flow<OrderEntity?> = orderDao.observeById(id)

    suspend fun getById(id: String): OrderEntity? = orderDao.getById(id)

    suspend fun create(
        clientId: String,
        masterId: String?,
        title: String,
        category: String,
        description: String?,
        materialsNote: String?,
        price: Double?,
        startedAt: Long?,
        finishedAt: Long?,
    ): String {
        val now = System.currentTimeMillis()
        val order = OrderEntity(
            id = UUID.randomUUID().toString(),
            clientId = clientId,
            masterId = masterId,
            title = title,
            category = category,
            description = description,
            materialsNote = materialsNote,
            status = OrderStatus.NEW,
            price = price,
            startedAt = startedAt,
            finishedAt = finishedAt,
            createdAt = now,
            updatedAt = now,
        )
        orderDao.upsert(order)
        syncScheduler.triggerSoon()
        return order.id
    }

    suspend fun update(
        existing: OrderEntity,
        clientId: String,
        masterId: String?,
        title: String,
        category: String,
        description: String?,
        materialsNote: String?,
        price: Double?,
        startedAt: Long?,
        finishedAt: Long?,
    ) {
        orderDao.upsert(
            existing.copy(
                clientId = clientId,
                masterId = masterId,
                title = title,
                category = category,
                description = description,
                materialsNote = materialsNote,
                price = price,
                startedAt = startedAt,
                finishedAt = finishedAt,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        syncScheduler.triggerSoon()
    }

    suspend fun updateStatus(existing: OrderEntity, status: OrderStatus) {
        orderDao.upsert(existing.copy(status = status, updatedAt = System.currentTimeMillis()))
        syncScheduler.triggerSoon()
    }

    suspend fun delete(order: OrderEntity) {
        orderDao.upsert(order.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        syncScheduler.triggerSoon()
    }
}
