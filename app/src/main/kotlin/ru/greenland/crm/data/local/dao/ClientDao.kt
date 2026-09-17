package ru.greenland.crm.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.greenland.crm.data.local.entity.ClientEntity

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE isDeleted = 0 ORDER BY fullName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    fun observeById(id: String): Flow<ClientEntity?>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: String): ClientEntity?

    @Upsert
    suspend fun upsert(client: ClientEntity)

    /** Для синхронизации — включая мягко удалённые (нужно отправить и их «надгробие»). */
    @Query("SELECT * FROM clients")
    suspend fun getAllForSync(): List<ClientEntity>
}
