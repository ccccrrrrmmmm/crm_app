package ru.greenland.crm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val ORDERS = "orders"
    const val ORDER_NEW_PATTERN = "orders/new?clientId={clientId}"
    const val ORDER_DETAIL = "orders/{orderId}"
    const val ORDER_EDIT = "orders/{orderId}/edit"

    const val CLIENTS = "clients"
    const val CLIENT_NEW = "clients/new"
    const val CLIENT_DETAIL = "clients/{clientId}"
    const val CLIENT_EDIT = "clients/{clientId}/edit"

    const val MASTERS = "masters"
    const val MASTER_NEW = "masters/new"
    const val MASTER_EDIT = "masters/{masterId}/edit"

    const val SETTINGS = "settings"

    fun orderNew(clientId: String? = null) = if (clientId != null) "orders/new?clientId=$clientId" else "orders/new"
    fun orderDetail(orderId: String) = "orders/$orderId"
    fun orderEdit(orderId: String) = "orders/$orderId/edit"
    fun clientDetail(clientId: String) = "clients/$clientId"
    fun clientEdit(clientId: String) = "clients/$clientId/edit"
    fun masterEdit(masterId: String) = "masters/$masterId/edit"
}

data class BottomTab(val route: String, val label: String, val icon: ImageVector)

val bottomTabs = listOf(
    BottomTab(Routes.ORDERS, "Заявки", Icons.AutoMirrored.Filled.Assignment),
    BottomTab(Routes.CLIENTS, "Клиенты", Icons.Filled.People),
    BottomTab(Routes.MASTERS, "Мастера", Icons.Filled.Engineering),
    BottomTab(Routes.SETTINGS, "Настройки", Icons.Filled.Settings),
)
