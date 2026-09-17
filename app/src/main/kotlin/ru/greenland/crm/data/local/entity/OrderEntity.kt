package ru.greenland.crm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "orders",
    indices = [Index("clientId"), Index("masterId"), Index("status")],
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val masterId: String?,
    val title: String,
    /** Тип работ: ремонт котла, промывка системы отопления и т.п. */
    val category: String,
    /** Комментарий к заявке. */
    val description: String?,
    /** Что нужно купить или уже было куплено для этой заявки. */
    val materialsNote: String?,
    val status: OrderStatus,
    val price: Double?,
    /** Время начала выполнения заявки, ставится мастером вручную. */
    val startedAt: Long?,
    /** Время окончания выполнения заявки, ставится мастером вручную. */
    val finishedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
)
