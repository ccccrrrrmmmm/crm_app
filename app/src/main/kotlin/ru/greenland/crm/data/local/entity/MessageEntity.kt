package ru.greenland.crm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Сообщение в переписке по заявке. Хранится локально и синхронизируется через GitHub. */
@Serializable
@Entity(tableName = "messages", indices = [Index("orderId")])
data class MessageEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val fromMaster: Boolean,
    val text: String,
    val createdAt: Long,
)
