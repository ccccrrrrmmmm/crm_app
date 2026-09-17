package ru.greenland.crm.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.greenland.crm.data.local.entity.MessageEntity

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE orderId = :orderId ORDER BY createdAt ASC")
    fun observeByOrder(orderId: String): Flow<List<MessageEntity>>

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Query("SELECT * FROM messages")
    suspend fun getAllForSync(): List<MessageEntity>
}
