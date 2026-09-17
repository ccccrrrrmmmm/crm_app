package ru.greenland.crm.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.greenland.crm.data.local.entity.SyncStateEntity

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE path = :path")
    suspend fun getByPath(path: String): SyncStateEntity?

    @Upsert
    suspend fun upsert(state: SyncStateEntity)

    @Query("SELECT remoteSha FROM sync_state WHERE path = :path")
    suspend fun getRemoteSha(path: String): String?
}
