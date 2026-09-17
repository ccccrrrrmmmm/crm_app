package ru.greenland.crm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "masters")
data class MasterEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val specialization: String?,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
)
