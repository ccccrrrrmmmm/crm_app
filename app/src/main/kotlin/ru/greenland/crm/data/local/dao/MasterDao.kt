package ru.greenland.crm.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.greenland.crm.data.local.entity.MasterEntity

@Dao
interface MasterDao {
    @Query("SELECT * FROM masters WHERE isDeleted = 0 ORDER BY fullName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<MasterEntity>>

    @Query("SELECT * FROM masters WHERE id = :id")
    fun observeById(id: String): Flow<MasterEntity?>

    @Query("SELECT * FROM masters WHERE id = :id")
    suspend fun getById(id: String): MasterEntity?

    @Upsert
    suspend fun upsert(master: MasterEntity)

    @Query("SELECT * FROM masters")
    suspend fun getAllForSync(): List<MasterEntity>
}
