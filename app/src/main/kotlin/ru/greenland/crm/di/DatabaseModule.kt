package ru.greenland.crm.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.greenland.crm.data.local.CrmDatabase
import ru.greenland.crm.data.local.dao.ClientDao
import ru.greenland.crm.data.local.dao.MasterDao
import ru.greenland.crm.data.local.dao.MessageDao
import ru.greenland.crm.data.local.dao.OrderDao
import ru.greenland.crm.data.local.dao.OrderPhotoDao
import ru.greenland.crm.data.local.dao.SyncStateDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CrmDatabase =
        Room.databaseBuilder(context, CrmDatabase::class.java, CrmDatabase.DB_NAME)
            // Проект ещё в активной разработке, миграций нет — при смене схемы просто пересоздаём БД.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideClientDao(database: CrmDatabase): ClientDao = database.clientDao()

    @Provides
    fun provideMasterDao(database: CrmDatabase): MasterDao = database.masterDao()

    @Provides
    fun provideOrderDao(database: CrmDatabase): OrderDao = database.orderDao()

    @Provides
    fun provideMessageDao(database: CrmDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideOrderPhotoDao(database: CrmDatabase): OrderPhotoDao = database.orderPhotoDao()

    @Provides
    fun provideSyncStateDao(database: CrmDatabase): SyncStateDao = database.syncStateDao()
}
