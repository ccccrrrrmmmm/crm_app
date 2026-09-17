package ru.greenland.crm.data.local

import androidx.room.TypeConverter
import ru.greenland.crm.data.local.entity.OrderStatus

class Converters {
    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = OrderStatus.valueOf(value)
}
