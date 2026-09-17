package ru.greenland.crm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Фото, снятое по заявке (что и как было сделано). Файл лежит в локальном хранилище приложения. */
@Serializable
@Entity(tableName = "order_photos", indices = [Index("orderId")])
data class OrderPhotoEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val filePath: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val isDeleted: Boolean = false,
)
