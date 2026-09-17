package ru.greenland.crm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.greenland.crm.data.local.dao.ClientDao
import ru.greenland.crm.data.local.dao.MasterDao
import ru.greenland.crm.data.local.dao.MessageDao
import ru.greenland.crm.data.local.dao.OrderDao
import ru.greenland.crm.data.local.dao.OrderPhotoDao
import ru.greenland.crm.data.local.dao.SyncStateDao
import ru.greenland.crm.data.local.entity.ClientEntity
import ru.greenland.crm.data.local.entity.MasterEntity
import ru.greenland.crm.data.local.entity.MessageEntity
import ru.greenland.crm.data.local.entity.OrderEntity
import ru.greenland.crm.data.local.entity.OrderPhotoEntity
import ru.greenland.crm.data.local.entity.SyncStateEntity

@Database(
    entities = [
        ClientEntity::class,
        MasterEntity::class,
        OrderEntity::class,
        MessageEntity::class,
        OrderPhotoEntity::class,
        SyncStateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CrmDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun masterDao(): MasterDao
    abstract fun orderDao(): OrderDao
    abstract fun messageDao(): MessageDao
    abstract fun orderPhotoDao(): OrderPhotoDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val DB_NAME = "greenland_crm.db"
    }
}
