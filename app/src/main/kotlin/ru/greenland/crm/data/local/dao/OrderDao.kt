package ru.greenland.crm.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.greenland.crm.data.local.entity.OrderEntity

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE clientId = :clientId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeByClient(clientId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun observeById(id: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: String): OrderEntity?

    @Upsert
    suspend fun upsert(order: OrderEntity)

    @Query("SELECT * FROM orders")
    suspend fun getAllForSync(): List<OrderEntity>
}
