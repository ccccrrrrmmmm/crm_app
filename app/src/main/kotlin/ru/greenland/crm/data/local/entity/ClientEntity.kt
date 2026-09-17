package ru.greenland.crm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val address: String?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
)
