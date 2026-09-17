package ru.greenland.crm.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.greenland.crm.data.local.entity.OrderPhotoEntity

@Dao
interface OrderPhotoDao {
    @Query("SELECT * FROM order_photos WHERE orderId = :orderId AND isDeleted = 0 ORDER BY createdAt ASC")
    fun observeByOrder(orderId: String): Flow<List<OrderPhotoEntity>>

    @Query("SELECT * FROM order_photos WHERE id = :id")
    suspend fun getById(id: String): OrderPhotoEntity?

    @Upsert
    suspend fun upsert(photo: OrderPhotoEntity)

    @Query("SELECT * FROM order_photos")
    suspend fun getAllForSync(): List<OrderPhotoEntity>
}
